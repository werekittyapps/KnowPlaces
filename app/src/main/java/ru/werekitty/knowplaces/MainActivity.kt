

package ru.werekitty.knowplaces

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.recyclerCityList
import ru.werekitty.knowplaces.models.City
import ru.werekitty.knowplaces.views.AddPlaceDialog

class MainActivity : AppCompatActivity(), AddPlaceDialog.Listener, View.OnClickListener,
    ListAdapter.TapAction, ListAdapter.DeleteAction {

    var list : MutableList<String> = mutableListOf()
    var listUUID : MutableList<String> = mutableListOf()
    private lateinit var viewAdapter: RecyclerView.Adapter<*>

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mDatabase: DatabaseReference
    private lateinit var mCity: City

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        MainLayout.alpha = 0.0F

        mAuth = FirebaseAuth.getInstance()
        mAuth.signInAnonymously().addOnSuccessListener {
            // Подключение выбранного города
            currentCityHandler()
        }
        mDatabase = FirebaseDatabase.getInstance().reference

        addPlaceBtn.isClickable = false


    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.CityNameText -> {
                goToAddCity()
            }
            R.id.addPlaceBtn -> {
                if (mCity.city != ""){
                    AddPlaceDialog().show(supportFragmentManager, "AddPlaceDialog")
                }
            }
        }
    }

    fun currentCityHandler(){
        mDatabase.child("currentcity/" + mAuth.currentUser!!.uid).addValueEventListener(ValueEventListenerAdapter {
            if (it.getValue(City::class.java) != null) {
                mCity = it.getValue(City::class.java)!!

                CityNameText.setText(mCity.city)

                temperatureText.isEnabled = true
                weatherImage.isEnabled = true
                addPlaceBtn.isClickable = true

                viewAdapter = ListAdapter(list, listUUID, "places/" + mAuth.currentUser!!.uid +
                        "/" + mCity.uuid, mAuth, mDatabase, this)
                recyclerHelper(this, recyclerCityList, viewAdapter,list)

                FirebaseHelper(mAuth,mDatabase).getPlaceList(list,listUUID,viewAdapter,"places/" +
                        mAuth.currentUser!!.uid + "/" + mCity.uuid)

                //val url =
                //    "https://api.apixu.com/v1/current.json?key=a1afeb9f0b4e4e8180091340190704&q=" +
                //        mCity.city.replace(" ", "%20")

                val url =
                    "http://api.weatherstack.com/current?access_key=8a1638da9c7a6a2f7ea0f013cd7bd3bc&query=" +
                            mCity.city.replace(" ", "%20")
                weatherHandler(temperatureText, weatherImage, mainProgressBar, MainLayout).weatherApi().execute(url)

            } else {
                temperatureText.isEnabled = false
                weatherImage.isEnabled = false
                addPlaceBtn.isClickable = false
                mainProgressBar.alpha = 0.0F
                MainLayout.alpha = 1.0F
            }
        })
    }

    override fun DialogHelper(text: String) {
        var place = removeSpaces(text).replace("\n", "")
        if (!place.isEmpty()){
            if (isThatUnic(list, place)){
                list.clear()
                listUUID.clear()
                FirebaseHelper(mAuth, mDatabase).addPlace("places/" + mAuth.currentUser!!.uid +
                        "/" + mCity.uuid, place, "", "")
            } else {
                Toast.makeText(this, "This place already added", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Incorrect place name", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onTap(text: String, uuid: String) {
        val intent = Intent(this, PlaceCardActivity::class.java)
        intent.putExtra("CityName", mCity.city)
        intent.putExtra("CityUUID", mCity.uuid)
        intent.putExtra("PlaceName", text)
        intent.putExtra("PlaceUUID", uuid)
        startActivity(intent)
    }

    override fun onDelete(text: String, uuid: String) {
        FirebaseHelper(mAuth, mDatabase).remove("places/" + mAuth.currentUser!!.uid +
                "/" + mCity.uuid + "/" + uuid)
    }

    fun goToAddCity(){
        val intent = Intent(this, AddCityActivity::class.java)
        startActivity(intent)
        finish()
    }

    fun ifNoConnection() {
        android.os.Handler().postDelayed({
            if (MainLayout.alpha == 0.0F) {
                Toast.makeText(
                    this,
                    "Check internet connection", Toast.LENGTH_LONG
                ).show()
            }
        }, 9000)
    }

}
