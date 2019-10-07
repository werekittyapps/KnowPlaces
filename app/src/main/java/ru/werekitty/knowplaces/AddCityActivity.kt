package ru.werekitty.knowplaces

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_add_city.*
import ru.werekitty.knowplaces.models.City
import ru.werekitty.knowplaces.views.AddCityDialog

class AddCityActivity : AppCompatActivity(), View.OnClickListener, AddCityDialog.Listener,
    ListAdapter.TapAction, ListAdapter.DeleteAction{

    var list : MutableList<String> = mutableListOf()
    var listUUID : MutableList<String> = mutableListOf()
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mDatabase: DatabaseReference
    private lateinit var mCity: City

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_city)

        mAuth = FirebaseAuth.getInstance()
        mAuth.signInAnonymously()
        mDatabase = FirebaseDatabase.getInstance().reference

        // Настройка списка
        viewAdapter = ListAdapter(list, listUUID, "cities/" + mAuth.currentUser!!.uid, mAuth, mDatabase, this)
        recyclerHelper(this, recyclerCityList, viewAdapter,list)

        // Чтение списка из Firebase
        FirebaseHelper(mAuth,mDatabase).getCityList(list,listUUID,viewAdapter,"cities/" + mAuth.currentUser!!.uid)

        currentCityHandler()

    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.addCityBtn -> {
                AddCityDialog().show(supportFragmentManager, "AddCityDialog")
            }
            R.id.doneBtn -> {
                goToMain()
            }
        }
    }

    override fun DialogHelper(text: String) {
        var city = removeSpaces(text).replace("\n", "")
        if (!city.isEmpty()){
            if (isThatUnic(list, city)){
                list.clear()
                listUUID.clear()
                FirebaseHelper(mAuth, mDatabase).addCity("cities/" + mAuth.currentUser!!.uid, city)
            } else {
                Toast.makeText(this, "This city already added", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Incorrect city name", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onTap(text: String, uuid: String) {
        AddCityTextView.text = text
        FirebaseHelper(mAuth, mDatabase).setCurrentCity(text, uuid)
    }

    override fun onDelete(text: String, uuid: String) {
        if (AddCityTextView.text.toString() == text){
            FirebaseHelper(mAuth, mDatabase).remove("cities/" + mAuth.currentUser!!.uid + "/" + uuid)
            FirebaseHelper(mAuth, mDatabase).remove("places/" + mAuth.currentUser!!.uid + "/" + uuid)
            FirebaseHelper(mAuth, mDatabase).remove("currentcity/" + mAuth.currentUser!!.uid)
            AddCityTextView.setText(R.string.choose_city)
        } else {
            FirebaseHelper(mAuth, mDatabase).remove("cities/" + mAuth.currentUser!!.uid + "/" + uuid)
            FirebaseHelper(mAuth, mDatabase).remove("places/" + mAuth.currentUser!!.uid + "/" + uuid)
        }
    }

    fun currentCityHandler(){
        mDatabase.child("currentcity/" + mAuth.currentUser!!.uid).addValueEventListener(ValueEventListenerAdapter {
            if (it.getValue(City::class.java) != null) {
                mCity = it.getValue(City::class.java)!!
                AddCityTextView.setText(mCity.city)
            }
        })
    }

    fun goToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

}
