package com.example.wingedworld.activities.activities

import android.Manifest
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.wingedworld.databinding.ActivityAddNewBirdObservationBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

//progress dialog
private lateinit var progressDialog: ProgressDialog
//View binding
private lateinit var binding: ActivityAddNewBirdObservationBinding
//firebase auth
private lateinit var auth: FirebaseAuth
//firebase user
private lateinit var user: FirebaseUser
//Image uri
private var imageUri: Uri? = null

private lateinit var fusedLocationClient: FusedLocationProviderClient

private val locationPermissionCode = 1

private lateinit var placesClient: PlacesClient



class AddNewBirdObservationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddNewBirdObservationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //init firebase auth
        auth = FirebaseAuth.getInstance()

        user = auth.currentUser!!

        //init progress dialog, will show while creating account
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)

        // Initialize the Places API
        Places.initialize(applicationContext, "AIzaSyA9543Ev1wo8ZS7hUehBD7rmz458xQbywg")
        placesClient = Places.createClient(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        binding.backBtn.setOnClickListener {
            startActivity(Intent(this, BirdsObservationsActivity::class.java))
            finish()
        }

        //Handle click, pick image from camera/gallery
        binding.profileIv.setOnClickListener {
            showImageAttachMenu()
        }

        //Handle click, begin update profile
        binding.addBirdObsBtn.setOnClickListener {
            getLocation()
        }
    }

    var latitude = 0.0
    var longitude = 0.0
    private fun getLocation() {
        Log.d("Location", "getLocation() called") // Add this line
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                100
            )
            return
        }

        val task = fusedLocationClient.lastLocation

        task.addOnSuccessListener { location ->
            if (location != null) {
                latitude = location.latitude
                longitude = location.longitude
                validateData(latitude,longitude) // Move the function call here
            }
        }
    }

    private var name =""
    private var count =""
    private fun validateData(latitude: Double, longitude: Double) {
        //Get data
        name = binding.nameEt.text.toString().trim()
        count = binding.countEt.text.toString().trim()


        //Validate data
        if (name.isEmpty()){
            Toast.makeText(this, "Enter Bird Name", Toast.LENGTH_SHORT).show()
        }else if (TextUtils.isEmpty(count)) {
            Toast.makeText(baseContext, "Enter Bird Count...", Toast.LENGTH_SHORT).show()
        }
        else{
            if (imageUri == null){
                updateBirdObs("",latitude,longitude)
            }
            else{
                uploadImage(latitude,longitude)
            }
        }
    }

    private fun uploadImage(latitude: Double, longitude: Double) {
        progressDialog.setMessage("Adding Bird Observation")
        progressDialog.show()

        val filePathAndName = "ProfileImages/"+ auth.uid

        val ref = FirebaseStorage.getInstance().getReference(filePathAndName)
        ref.putFile(imageUri!!)
            .addOnSuccessListener { taskSnapshot->
                progressDialog.dismiss()
                val uriTask: Task<Uri> = taskSnapshot.storage.downloadUrl
                while(!uriTask.isSuccessful);
                val uploadedImageUrl = "${uriTask.result}"

                updateBirdObs(uploadedImageUrl,latitude,longitude)
            }

    }

    private fun updateBirdObs(uploadedImageUrl: String,latitude: Double, longitude: Double) {
        progressDialog.setMessage("Adding Bird Observation")
        progressDialog.show()

        val timestamp = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        val uid = auth.uid

        //Setup info to update to db
        val hashmap: HashMap<String, Any> = HashMap()
        hashmap["id"] = "$timestamp"
        hashmap["uid"] = uid.toString()
        hashmap["name"] = "$name"
        hashmap["count"] = "$count"
        hashmap["latitude"] = latitude
        hashmap["longitude"] = longitude
        hashmap["date"] = dateFormat.format(Date(timestamp))
        hashmap["timestamp"] = timestamp
        if (imageUri != null){
            hashmap["profileImage"] = uploadedImageUrl
        }
        if (latitude != null && longitude != null) {
            // Perform reverse geocoding to get city or suburb based on latitude and longitude
            val cityOrSuburb = reverseGeocode(latitude, longitude)
            hashmap["location"] = cityOrSuburb
        }

        //update db
        val ref = FirebaseDatabase.getInstance().getReference("BirdObservation")
        ref.child("$timestamp")
            .updateChildren(hashmap)
            .addOnSuccessListener {
                //Profile updated
                progressDialog.dismiss()
                Toast.makeText(this, "Bird Observation Added Successfully", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, BirdsObservationsActivity::class.java))
                finish()
            }
            .addOnFailureListener {e ->
                //Failed to upload image
                progressDialog.dismiss()
                Toast.makeText(
                    this,
                    "Failed To Add Bird Observation Due To ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun reverseGeocode(latitude: Double, longitude: Double): String {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses?.isNotEmpty() == true) {
                // Get city or suburb from the first address
                val locality = addresses[0]?.locality
                val subLocality = addresses[0]?.subLocality
                return locality ?: subLocality ?: ""
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return ""
    }

    private fun showImageAttachMenu(){

        //Show popup menu with options Camera, Gallery to pick image

        //Setup popup menu
        val popupMenu = PopupMenu(this, binding.profileIv)
        popupMenu.menu.add(Menu.NONE,0,0,"Camera")
        popupMenu.menu.add(Menu.NONE,1,1,"Gallery")
        popupMenu.show()

        //Handle popup menu item click
        popupMenu.setOnMenuItemClickListener { item->

            val id = item.itemId
            if (id == 0){
                pickImageCamera()
            }
            else if(id ==1){
                pickImageGallery()
            }

            true
        }
    }

    private fun pickImageGallery() {

        //Intent to pick image from gallery
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryActivityResultLauncher.launch(intent)
    }

    private fun pickImageCamera() {
        //Intent to pick image from camera
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE,"Temp Title")
        values.put(MediaStore.Images.Media.DESCRIPTION,"Temp Description")

        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraActivityResultLauncher.launch(intent)
    }

    private val cameraActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ActivityResultCallback<ActivityResult>{ result ->
            if(result.resultCode == RESULT_OK){
                val data = result.data

                binding.profileIv.setImageURI(imageUri)
            }
            else{
                Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    )

    private val galleryActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ActivityResultCallback<ActivityResult>{ result ->
            if(result.resultCode == RESULT_OK){
                val data = result.data
                imageUri = data!!.data

                binding.profileIv.setImageURI(imageUri)
            }
            else{
                Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    )

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}