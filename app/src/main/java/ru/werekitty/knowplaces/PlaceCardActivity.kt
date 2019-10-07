package ru.werekitty.knowplaces

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_place_card.*
import ru.werekitty.knowplaces.models.Place

class PlaceCardActivity : AppCompatActivity(), View.OnClickListener {

    val REQUST_IMAGE_GET = 1
    val RESULT_IMAGE_GET = 17
    var editBtnState = 0

    private lateinit var mCityName : String
    private lateinit var mCityUUID : String
    private lateinit var mPlaceName : String
    private lateinit var mPlaceUUID : String
    private lateinit var mPlace: Place

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mDatabase: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_place_card)

        mCityName = intent.getStringExtra("CityName")!!
        mCityUUID = intent.getStringExtra("CityUUID")!!
        mPlaceName = intent.getStringExtra("PlaceName")!!
        mPlaceUUID = intent.getStringExtra("PlaceUUID")!!

        mAuth = FirebaseAuth.getInstance()
        mAuth.signInAnonymously()
        mDatabase = FirebaseDatabase.getInstance().reference

        placeName.text = mPlaceName
        placeNameEdit.setText(mPlaceName)

        currentPlaceHandler()
        disableEdits()
    }



    override fun onClick(view: View) {
        when (view.id) {
            R.id.AddImageBtn -> {
                addPicture()
            }
            R.id.placeEditBtn -> {
                btnState()
            }
            R.id.placeBackBtn -> {
                goToMain()
            }
        }
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUST_IMAGE_GET && resultCode == Activity.RESULT_OK) {
            if (data != null){
                val uri = data!!.data!!
                GlideApp.with(this).load(uri).centerCrop().into(placeImage)
                mPlace = mPlace.copy(imageUri = uri.toString())
                updatePlace()
            }
        }
    }

    fun currentPlaceHandler(){
        mDatabase.child("places/" + mAuth.currentUser!!.uid + "/" +
                mCityUUID + "/" + mPlaceUUID).addValueEventListener(ValueEventListenerAdapter {
            if (it.getValue(Place::class.java) != null) {
                mPlace = it.getValue(Place::class.java)!!

                if(mPlace.description != "") {
                    placeDescription.setText(mPlace.description)
                    placeDescriptionEdit.setText(mPlace.description)
                }
                if(mPlace.imageUri != "") {
                    GlideApp.with(this).load(mPlace.imageUri).centerCrop().into(placeImage)
                } else {
                    GlideApp.with(this).load(R.drawable.place).centerCrop().into(placeImage)
                }
            }
        })
    }

    fun addPicture(){
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, REQUST_IMAGE_GET)
            onActivityResult(REQUST_IMAGE_GET, RESULT_IMAGE_GET, intent)
        }
    }

    fun btnState() {
        if (editBtnState == 0) {
            // переключение на режим редактирования
            editBtnState = 1
            placeEditBtn.setText("Save")
            enableEdits()
            disableTexts()
        } else {
            val place = removeSpaces(placeNameEdit.text.toString()).replace("\n", "")
            if (!place.isEmpty()){
                // выход из режима редактирования
                editBtnState = 0
                placeEditBtn.setText("Edit")
                onSave(place)
                disableEdits()
                enableTexts()
            } else {
                Toast.makeText(this, "Incorrect place name", Toast.LENGTH_SHORT).show()
            }

        }
    }

    fun onSave(place: String) {
        if(!(placeNameEdit.text.toString() == mPlace.place && placeDescriptionEdit.text.toString() == mPlace.description)) {
            mPlace = mPlace.copy(place = place, description = placeDescriptionEdit.text.toString())
            placeName.setText(place)
            if (placeDescriptionEdit.text.toString() == "") {
                placeDescription.text = "Description"
            } else {
                placeDescription.text = placeDescriptionEdit.text.toString()
            }
            updatePlace()
        }
    }

    fun updatePlace(){
        FirebaseHelper(mAuth, mDatabase).updPlace("places/" + mAuth.currentUser!!.uid +
                "/" + mCityUUID, mPlace.place, mPlace.uuid, mPlace.description, mPlace.imageUri)
    }

    fun disableEdits() {
        placeNameEdit.isEnabled = false
        placeNameEdit.alpha = 0.0F
        placeDescriptionEdit.isEnabled = false
        placeDescriptionEdit.alpha = 0.0F
    }

    fun enableEdits() {
        placeNameEdit.isEnabled = true
        placeNameEdit.alpha = 1.0F
        placeDescriptionEdit.isEnabled = true
        placeDescriptionEdit.alpha = 1.0F
    }

    fun disableTexts() {
        placeName.isEnabled = false
        placeName.alpha = 0.0F
        placeDescription.isEnabled = false
        placeDescription.alpha = 0.0F
    }

    fun enableTexts() {
        placeName.isEnabled = true
        placeName.alpha = 1.0F
        placeDescription.isEnabled = true
        placeDescription.alpha = 1.0F
    }

    fun goToMain(){
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

}

