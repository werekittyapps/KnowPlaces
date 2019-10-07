package ru.werekitty.knowplaces

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_add_city.*
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONException
import org.json.JSONObject
import ru.werekitty.knowplaces.models.City
import ru.werekitty.knowplaces.models.Place
import java.io.FileNotFoundException
import java.net.HttpURLConnection
import java.net.URL

@GlideModule
class CustomGlideModule : AppGlideModule()

// Адаптер списка
class ListAdapter(private val list: MutableList<String>,
                  private val listUUID: MutableList<String>,
                  root: String, auth: FirebaseAuth, database: DatabaseReference,
                  context: Context
                    ) : RecyclerView.Adapter<ListAdapter.ListViewHolder>() {

    val mAuth = auth
    val mDatabase = database
    val mRoot = root
    val mContext = context

    private lateinit var mTapAction : TapAction
    private lateinit var mDeleteAction: DeleteAction

    interface TapAction {
        fun onTap(text: String, uuid: String){
        }
    }

    interface DeleteAction {
        fun onDelete(text: String, uuid: String){
        }
    }

    class ListViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val listText: TextView = v.findViewById(R.id.listTextView)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ListAdapter.ListViewHolder {
        val v = LayoutInflater.from(viewGroup.context).inflate(R.layout.list_helper, viewGroup, false)
        return ListViewHolder(v)
    }

    override fun onBindViewHolder(viewHolder: ListViewHolder, position: Int) {
        viewHolder.listText.text = list[position]
        viewHolder.listText.setOnClickListener{
            mTapAction = mContext as TapAction
            mTapAction.onTap(list[position], listUUID[position])
        }
    }

    fun removeItem(position: Int, viewHolder: RecyclerView.ViewHolder) {
        mDeleteAction = mContext as DeleteAction
        mDeleteAction.onDelete(list[position], listUUID[position])
        list.clear()
        listUUID.clear()

        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return list.size
    }
}

// Обработка свайпа позиции списка
class TouchHelper(context: Context, private val list: MutableList<String>, adapter: RecyclerView.Adapter<*>) {

    var colorDrawableBackground: ColorDrawable = ColorDrawable(Color.parseColor("#ff0000"))
    var deleteIcon: Drawable = ContextCompat.getDrawable(context, R.drawable.ic_delete)!!


    val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, viewHolder2: RecyclerView.ViewHolder): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDirection: Int) {
            (adapter as ListAdapter).removeItem(viewHolder.adapterPosition, viewHolder)
        }

        override fun onChildDraw(
            c: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean
        ) {
            val itemView = viewHolder.itemView
            val iconMarginVertical = (viewHolder.itemView.height - deleteIcon.intrinsicHeight) / 2

            if (dX > 0) {
                colorDrawableBackground.setBounds(itemView.left, itemView.top, dX.toInt(), itemView.bottom)
                deleteIcon.setBounds(itemView.left + iconMarginVertical, itemView.top + iconMarginVertical,
                    itemView.left + iconMarginVertical + deleteIcon.intrinsicWidth, itemView.bottom - iconMarginVertical)
            } else {
                colorDrawableBackground.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
                deleteIcon.setBounds(itemView.right - iconMarginVertical - deleteIcon.intrinsicWidth, itemView.top + iconMarginVertical,
                    itemView.right - iconMarginVertical, itemView.bottom - iconMarginVertical)
                deleteIcon.level = 0
            }

            colorDrawableBackground.draw(c)

            c.save()

            if (dX > 0)
                c.clipRect(itemView.left, itemView.top, dX.toInt(), itemView.bottom)
            else
                c.clipRect(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)

            deleteIcon.draw(c)

            c.restore()

            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }
    }

    val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
}

// Для быстрой настройки списка
fun recyclerHelper (context: Context, recyclerView: RecyclerView, adapterView: RecyclerView.Adapter<*>, list: MutableList<String>){
    recyclerView.apply {
        setHasFixedSize(true)
        adapter = adapterView
        layoutManager = LinearLayoutManager(this.context)
        addItemDecoration(DividerItemDecoration(this.context, DividerItemDecoration.VERTICAL))
    }
    TouchHelper(context, list, adapterView).itemTouchHelper.attachToRecyclerView(recyclerView)
}


// Использование API прогноза погоды
class weatherHandler(textView: TextView, imageView: ImageView, progressBar: ProgressBar, layout: ConstraintLayout) {

    val mTextView = textView
    val mImageView = imageView
    val mProgressBar = progressBar
    val mLayout = layout

    inner class weatherApi() : AsyncTask<String, String, String>() {
        override fun doInBackground(vararg url: String?): String {

            var text: String
            val connection = URL(url[0]).openConnection() as HttpURLConnection
            try {
                connection.connect()
                text =
                    connection.inputStream.use { it.reader().use { reader -> reader.readText() } }
            } catch (e: FileNotFoundException) {
                text = "Incorrect city name"
            } finally {
                connection.disconnect()
            }

            return text

        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            mTextView.setTextSize(14.0F)
            weatherJson(result)
        }
    }

    private fun weatherJson(jsonString: String?) {

        if (jsonString != "Incorrect city name") {
            val full = JSONObject(jsonString)
            try {
                val current = JSONObject("" + full.get("current"))
                val temperature = current.get("temperature")
                val weatherImage = current.get("weather_icons").toString()
                    .replace("[","")
                    .replace("]", "")
                    .replace("\\", "")
                    .replace("\"", "")
                val image = "https://cdn.apixu.com/weather/64x64/day/389.png"

                mTextView.setText("" + temperature + "℃")
                mTextView.setTextSize(36.0F)
                weatherIcon().execute(weatherImage)
                mProgressBar.alpha = 0.0F
                mLayout.alpha = 1.0F
            } catch (e: JSONException) {
                mProgressBar.alpha = 0.0F
                mLayout.alpha = 1.0F
            }
        } else {
            mTextView.setText("Incorrect city name")
        }

    }

    /*
    // APIXU.com API
    private fun weatherJson(jsonString: String?) {

        if (jsonString != "Incorrect city name") {
            val full = JSONObject(jsonString)
            try {
                val current = JSONObject("" + full.get("current"))
                val temperature = current.get("temp_c")
                val condition = JSONObject("" + current.get("condition"))
                val weatherImage = condition.get("icon")

                mTextView.setText("" + temperature + "℃")
                mTextView.setTextSize(36.0F)
                weatherIcon().execute("https:" + weatherImage)
                println(weatherImage)
            } catch (e: JSONException) {
            }
        } else {
            mTextView.setText("Incorrect city name")
        }

    }*/

    inner class weatherIcon() : AsyncTask<String, Void, Bitmap>() {
        override fun doInBackground(vararg url: String?): Bitmap {

            var bitmap: Bitmap
            val connection = URL(url[0]).openConnection() as HttpURLConnection
            try {
                connection.connect()
                bitmap = BitmapFactory.decodeStream(connection.inputStream)
            } finally {
                connection.disconnect()
            }

            return bitmap

        }

        override fun onPostExecute(result: Bitmap?) {
            super.onPostExecute(result)
            mImageView.setImageBitmap(result)
        }
    }
}

// Обрабатывает чтение из библиотеки
class ValueEventListenerAdapter(val handler: (DataSnapshot) -> Unit) : ValueEventListener {
    private val TAG = "ValueEventListenerAdapt"

    override fun onDataChange(data: DataSnapshot) {
        handler(data)
    }

    override fun onCancelled(error: DatabaseError) {
        Log.e(TAG, "onCancelled: ", error.toException())
    }
}

// Есть ли в списке (list) такое слово (name)
fun isThatUnic(list: MutableList<String>, name: String) : Boolean {

    var isUnic = true
    if(name == "Choose the City") isUnic = false
    for (i in list){
        if (name == i) isUnic = false
    }
    return isUnic
}

// Убирает лишние пробелы
fun removeSpaces(text: String) : String {
    var result = ""
    var prevChar = ""
     for (char in text) {
         if(!(prevChar == " " && char == ' ')){
             result += char
         }
         prevChar = char.toString()
     }
    return result.trim()
}

// Чтение, запись и удаление данных Firebase
class FirebaseHelper(auth: FirebaseAuth, database: DatabaseReference) {
    private val TAG = "FirebaseHelper"

    var mAuth = auth
    var mDatabase = database

    fun addCity(root: String, cityName: String) {
        val key = mDatabase.child(root).push().key
        mDatabase.child(root).child(key!!).setValue(City(cityName, key)).addOnCompleteListener {
            if (it.isSuccessful){
                Log.d(TAG, "addCity: success")
            } else {
                Log.d(TAG, "addCity: failure", it.exception)
            }
        }
    }

    fun addPlace(root: String, placeName: String, description: String, imageUri: String) {
        val key = mDatabase.child(root).push().key
        mDatabase.child(root).child(key!!).setValue(Place(placeName, key, description, imageUri)).addOnCompleteListener {
            if (it.isSuccessful){
                Log.d(TAG, "addPlace: success")
            } else {
                Log.d(TAG, "addPlace: failure", it.exception)
            }
        }
    }

    fun updPlace(root: String, placeName: String, placeUUID: String, description: String, imageUri: String) {
        mDatabase.child(root).child(placeUUID).setValue(Place(placeName, placeUUID, description, imageUri)).addOnCompleteListener {
            if (it.isSuccessful){
                Log.d(TAG, "updPlace: success")
            } else {
                Log.d(TAG, "updPlace: failure", it.exception)
            }
        }
    }

    fun setCurrentCity(cityName: String, cityKey: String) {
        mDatabase.child("currentcity/" + mAuth.currentUser!!.uid).setValue(City(cityName, cityKey)).addOnCompleteListener {
            if (it.isSuccessful){
                Log.d(TAG, "setCurrentCity: success")
            } else {
                Log.d(TAG, "setCurrentCity: failure", it.exception)
            }
        }
    }

    fun getCityList(list: MutableList<String>, listUUID: MutableList<String>, viewAdapter: RecyclerView.Adapter<*>, root: String) {
        var mCity: City
        mDatabase.child(root).addValueEventListener(ValueEventListenerAdapter{
            it.children.map {
                mCity = it.getValue(City::class.java)!!
                list.add(mCity.city)
                listUUID.add(mCity.uuid)
                viewAdapter.notifyDataSetChanged()
            }
        })
    }

    fun getPlaceList(list: MutableList<String>, listUUID: MutableList<String>, viewAdapter: RecyclerView.Adapter<*>, root: String) {
        var mPlace: Place
        mDatabase.child(root).addValueEventListener(ValueEventListenerAdapter{
            it.children.map {
                mPlace = it.getValue(Place::class.java)!!
                list.add(mPlace.place)
                listUUID.add(mPlace.uuid)
                viewAdapter.notifyDataSetChanged()
            }
        })
    }

    fun remove(root: String) {
        mDatabase.child(root).removeValue().addOnCompleteListener {

            if (it.isSuccessful){
                Log.d(TAG, "remove: success")
            } else {
                Log.d(TAG, "remove: failure", it.exception)
            }
        }
    }
}