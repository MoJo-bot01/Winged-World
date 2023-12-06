package com.example.wingedworld.activities.activities

import android.Manifest
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.wingedworld.R
import com.example.wingedworld.activities.Hotspot
import com.example.wingedworld.databinding.ActivityMapBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.PendingResult
import com.google.maps.model.DirectionsResult
import com.google.maps.model.TravelMode
import java.io.InputStreamReader
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

//View binding
private lateinit var binding: ActivityMapBinding

//firebase auth
private lateinit var auth: FirebaseAuth

//firebase user
private lateinit var user: FirebaseUser

//progress dialog
private lateinit var progressDialog: ProgressDialog

private var isMapReady = false
private var lastKnownLocation: Location? = null
private val defaultLocation = LatLng(-26.195246, 28.034088)
private var locationPermissionGranted = true
private const val locationPermissionCode = 1
private lateinit var placesClient: PlacesClient
private var currentPolyline: Polyline? = null
private lateinit var mMap: GoogleMap
private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

val hotspotList = mutableListOf<Hotspot>()

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //init firebase auth
        auth = FirebaseAuth.getInstance()

        user = auth.currentUser!!

        //init progress dialog, will show while creating account
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Initialize the Places API
        Places.initialize(applicationContext, "AIzaSyA9543Ev1wo8ZS7hUehBD7rmz458xQbywg")
        placesClient = Places.createClient(this)

        binding.backBtn.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        binding.mapSettBtn.setOnClickListener {
            startActivity(Intent(this, MapSettingsActivity::class.java))
            finish()
        }

        binding.observationButton.setOnClickListener {
            startActivity(Intent(this, BirdsObservationsActivity::class.java))
            finish()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        // Enable user location tracking and move camera to current location
        checkLocationPermission()
        checkDistanceUnits()
        loadUserBirdObsOnMap()
        getDeviceLocation(mMap!!)
        updateLocationUI()

        mMap?.setOnMarkerClickListener { marker ->
            showMarkerOptions(marker)
            true // Consume the event
        }
    }

    data class BirdObservation(
        val latitude: Double=0.0,
        val longitude: Double=0.0
    )

    private fun loadUserBirdObsOnMap() {
        val locations : MutableList<BirdObservation> = mutableListOf()

        // Initialize the map fragment in your layout
        val mapFragment1 = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment

        val ref = FirebaseDatabase.getInstance().getReference("BirdObservation")

        ref.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (snapshot in dataSnapshot.children){
                    //Get data
                    val id = "${snapshot.child("id").value}"
                    val uid = "${snapshot.child("uid").value}"
                    val name = "${snapshot.child("name").value}"
                    val profileImage = "${snapshot.child("profileImage").value}"
                    val date = "${snapshot.child("date").value}"
                    val count = "${snapshot.child("count").value}"
                    val latitude = snapshot.child("latitude").getValue(Double::class.java)
                    val longitude = snapshot.child("latitude").getValue(Double::class.java)
                    val location = "${snapshot.child("location").value}"

                    // Ensure that the observation is not null
                    if (latitude != null && longitude != null) {
                        val observation = BirdObservation(
                            latitude,
                            longitude
                        )
                        locations.add(observation)
                    }
                }

                mapFragment1.getMapAsync { googleMap ->
                    // Iterate through locations and add markers
                    for (location in locations) {
                        googleMap.addMarker(
                            MarkerOptions()
                                .position(LatLng(location.latitude, location.longitude))
                                .title("Your Bird Observation")
                        )
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {

            }

        })
    }

    private fun showMarkerOptions(marker: Marker) {
        val builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.marker_options_dialog, null)
        builder.setView(dialogView)
        val alertDialog = builder.create()

        val hotspotName = dialogView.findViewById<TextView>(R.id.tv1)
        val directionsOption = dialogView.findViewById<Button>(R.id.directionsOption)
        val routeOption = dialogView.findViewById<Button>(R.id.showRouteOption)
        val cancelButton = dialogView.findViewById<ImageButton>(R.id.backBtn)

        hotspotName.text = marker.title

        // Calculate distance and duration
        val currentLocation = LatLng(mMap.myLocation.latitude, mMap.myLocation.longitude)
        val destination = marker.position

        val calculatedDistance = calculateHaversineDistance(
            currentLocation.latitude,
            currentLocation.longitude,
            destination.latitude,
            destination.longitude
        )

        //init firebase auth
        auth = FirebaseAuth.getInstance()

        //init firebase user
        user = auth.currentUser!!

        val uid = auth.uid
        val ref = FirebaseDatabase.getInstance().getReference("MapSettings")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (childSnapshot in snapshot.children){
                    //Get Data
                    val distanceNum = childSnapshot.child("distanceNum").getValue(String::class.java)
                    val distanceUnit = childSnapshot.child("distanceUnit").getValue(String::class.java)
                    val user = childSnapshot.child("uid").getValue(String::class.java)

                    if(uid == user){
                        if (distanceUnit != null){
                            // Convert the distance based on the user's preference
                            val formattedDistance = if (distanceUnit == "ml") {
                                val miles = calculatedDistance / 1609.34 // Convert meters to miles
                                String.format("%.2f mi", miles)
                            } else {
                                val kilometers = calculatedDistance / 1000 // Convert meters to kilometers
                                String.format("%.2f km", kilometers)
                            }

                            val speedMetersPerSecond = 15.0 // Adjust this based on your assumptions
                            val durationInSeconds = calculatedDistance / speedMetersPerSecond
                            val hours = durationInSeconds.toInt() / 3600
                            val minutes = (durationInSeconds.toInt() % 3600) / 60
                            val formattedDuration = if (hours > 0) {
                                "$hours hours $minutes minutes"
                            } else {
                                "$minutes minutes"
                            }
                            // Update the TextViews with the calculated values
                            dialogView.findViewById<TextView>(R.id.tv6)?.text = "$formattedDistance"
                            dialogView.findViewById<TextView>(R.id.tv8)?.text = "$formattedDuration"
                        }
                    }
                    else{

                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@MapActivity,
                    "Error loading map settings due to $error",
                    Toast.LENGTH_SHORT
                ).show()
            }

        })

        cancelButton.setOnClickListener {
            alertDialog.dismiss()
        }

        directionsOption.setOnClickListener {
            alertDialog.dismiss()
            navigateToHotspot(marker.position)
        }

        routeOption.setOnClickListener {
            alertDialog.dismiss()
            requestDirections(marker.position)
        }
        alertDialog.show()
    }

    private fun navigateToHotspot(position: LatLng) {
        val latitude = position.latitude
        val longitude = position.longitude

        val uri = "google.navigation:q=$latitude,$longitude"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        intent.setPackage("com.google.android.apps.maps")

        // Check if Google Maps is installed
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "Google Maps is not installed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun calculateHaversineDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val radius = 6371 // Earth radius in kilometers

        // Convert latitude and longitude from degrees to radians
        val lat1Rad = Math.toRadians(lat1)
        val lon1Rad = Math.toRadians(lon1)
        val lat2Rad = Math.toRadians(lat2)
        val lon2Rad = Math.toRadians(lon2)

        // Haversine formula
        val dLat = lat2Rad - lat1Rad
        val dLon = lon2Rad - lon1Rad

        val a = sin(dLat / 2).pow(2) + cos(lat1Rad) * cos(lat2Rad) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return radius * c * 1000 // Distance in meters
    }

    private fun calculateHaversineDistance1(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val radius = 6371.0 // Earth radius in kilometers

        // Convert latitude and longitude from degrees to radians
        val lat1Rad = Math.toRadians(lat1)
        val lon1Rad = Math.toRadians(lon1)
        val lat2Rad = Math.toRadians(lat2)
        val lon2Rad = Math.toRadians(lon2)

        // Haversine formula
        val dLat = lat2Rad - lat1Rad
        val dLon = lon2Rad - lon1Rad

        val a = sin(dLat / 2).pow(2) + cos(lat1Rad) * cos(lat2Rad) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return radius * c // Distance in kilometers
    }

    private fun requestDirections(selectedMarkerLatLng: LatLng) {
        progressDialog.setMessage("Loading Route...")
        progressDialog.show()
        val directionsApi = GeoApiContext.Builder()
            .apiKey("AIzaSyA9543Ev1wo8ZS7hUehBD7rmz458xQbywg")
            .build()

        val origin = "${lastKnownLocation!!.latitude}, ${lastKnownLocation!!.longitude}"
        val destination = "${selectedMarkerLatLng.latitude}, ${selectedMarkerLatLng.longitude}"

        DirectionsApi.newRequest(directionsApi)
            .origin(origin)
            .destination(destination)
            .mode(TravelMode.DRIVING)
            .avoid(DirectionsApi.RouteRestriction.TOLLS)
            .setCallback(object : PendingResult.Callback<DirectionsResult?> {
                override fun onResult(result: DirectionsResult?) {
                    if (result != null && result.routes.isNotEmpty()) {
                        val decodedPath = decodePolyline(result.routes[0].overviewPolyline.decodePath())
                        runOnUiThread {
                            progressDialog.dismiss()
                            drawRouteOnMap(decodedPath)
                        }
                    } else {
                    }
                }

                override fun onFailure(e: Throwable?) {
                }
            })
    }

    private fun updateLocationUI() {
        if (mMap == null) {
            return
        }
        try {
            if (locationPermissionGranted) {
                mMap?.isMyLocationEnabled = true
                mMap?.uiSettings?.isMyLocationButtonEnabled = true
            } else {
                mMap?.isMyLocationEnabled = false
                mMap?.uiSettings?.isMyLocationButtonEnabled = false
                lastKnownLocation = null
                checkLocationPermission()
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    private fun getDeviceLocation(mMap: GoogleMap) {
        try {
            if (locationPermissionGranted) {
                val locationResult = fusedLocationProviderClient.lastLocation
                locationResult.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        lastKnownLocation = task.result
                        if (lastKnownLocation != null) {
                            mMap?.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(
                                        lastKnownLocation!!.latitude,
                                        lastKnownLocation!!.longitude
                                    ), 15.toFloat()
                                )
                            )
                        }
                    } else {
                        Log.d(ContentValues.TAG, "Current location is null. Using defaults.")
                        Log.e(ContentValues.TAG, "Exception: %s", task.exception)
                        mMap?.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(defaultLocation, 15.toFloat())
                        )
                        mMap?.uiSettings?.isMyLocationButtonEnabled = false
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    private fun checkDistanceUnits() {
        //init firebase auth
        auth = FirebaseAuth.getInstance()

        val uid = auth.uid
        val ref = FirebaseDatabase.getInstance().getReference("MapSettings")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var distanceNum: String? = null // Initialize distanceNum
                for (childSnapshot in snapshot.children) {
                    //Get Data
                    distanceNum = childSnapshot.child("distanceNum").getValue(String::class.java)
                    val distanceUnit = childSnapshot.child("distanceUnit").getValue(String::class.java)
                    val user = childSnapshot.child("uid").getValue(String::class.java)


                    if(uid == user){
                        val distanceValue = distanceNum?.toDoubleOrNull()
                        if (distanceValue != null){
                            if (distanceValue >= 1){
                                // Show hotspots based on distanceNum
                                val filteredHotspots = distanceNum?.let { getFilteredMarkers(it) }
                                filteredHotspots?.start()
                                filteredHotspots?.join()
                            }
                            else if (distanceValue <=0){
                                // Show all hotspots
                                val dNum = "250"
                                val allHotspots = getMarkers(dNum)
                                allHotspots.start()
                                allHotspots.join()
                            }
                        }else {
                            val dNum = "250"
                            val allHotspots = getMarkers(dNum)
                            allHotspots.start()
                            allHotspots.join()
                        }
                    }else {
                        val dNum = "250"
                        val allHotspots = getMarkers(dNum)
                        allHotspots.start()
                        allHotspots.join()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@MapActivity,
                    "Error loading map settings due to $error",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun getFilteredMarkers(distanceNum: String): Thread {
        hotspotList.clear() // Clear the list before populating with new data

        val lat = lastKnownLocation!!.latitude
        val long = lastKnownLocation!!.longitude

        return Thread{
            val url = URL("https://api.ebird.org/v2/ref/hotspot/geo?fmt=json&lat=${lat}&lng=${long}&dist=$distanceNum")

            val connection = url.openConnection() as HttpsURLConnection

            if (connection.responseCode == 200) {
                val inputSystem = connection.inputStream
                val innputStreamReader = InputStreamReader(inputSystem, "UTF-8")

                val gson = Gson()
                val jsonArray = gson.fromJson(innputStreamReader, JsonArray::class.java)

                jsonArray.forEach { element ->
                    val log = element.asJsonObject["lng"].asDouble
                    val lat = element.asJsonObject["lat"].asDouble
                    val name = element.asJsonObject["locName"].asString
                    val hotspot = Hotspot(log, lat, name,1)
                    hotspotList.add(hotspot)
                }
            }

            // Now, update the UI on the main thread
            runOnUiThread {
                for (element in hotspotList) {
                    mMap?.addMarker(
                        MarkerOptions()
                            .position(LatLng(element.lat, element.log))
                            .title(element.name)
                            .snippet(element.name)
                    )
                }
            }
        }
    }

    private fun getMarkers(dNum: String): Thread {

        return Thread {
            val lat = lastKnownLocation!!.latitude
            val long = lastKnownLocation!!.longitude

            val url = URL("https://api.ebird.org/v2/ref/hotspot/geo?fmt=json&lat=${lat}&lng=${long}&dist=$dNum")

            val connection = url.openConnection() as HttpsURLConnection

            if (connection.responseCode == 200) {
                val inputSystem = connection.inputStream
                val innputStreamReader = InputStreamReader(inputSystem, "UTF-8")

                val gson = Gson()
                val jsonArray = gson.fromJson(innputStreamReader, JsonArray::class.java)

                jsonArray.forEach { element ->
                    val log = element.asJsonObject["lng"].asDouble
                    val lat = element.asJsonObject["lat"].asDouble
                    val name = element.asJsonObject["locName"].asString
                    val hotspot = Hotspot(log, lat, name, 1)
                    hotspotList.add(hotspot)
                }

                // Now, update the UI on the main thread
                runOnUiThread {
                    for (element in hotspotList) {
                        mMap?.addMarker(
                            MarkerOptions()
                                .position(LatLng(element.lat, element.log))
                                .title(element.name)
                                .snippet(element.name)
                        )
                    }
                }
            }
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            enableMyLocation()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                locationPermissionCode
            )
        }
    }

    private fun moveCameraToCurrentUserLocation() {
        if (mMap.isMyLocationEnabled && mMap.myLocation != null) {
            val userLocation = LatLng(
                mMap.myLocation.latitude,
                mMap.myLocation.longitude
            )
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
        }
    }

    private fun enableMyLocation() {
        if (isMapReady) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            mMap.isMyLocationEnabled = true
            moveCameraToCurrentUserLocation()
        }
    }

    private fun decodePolyline(encodedPath: List<com.google.maps.model.LatLng>): List<LatLng> {
        val path = ArrayList<LatLng>()
        for (latLng in encodedPath) {
            path.add(LatLng(latLng.lat, latLng.lng))
        }
        return path
    }

    private fun drawRouteOnMap(path: List<LatLng>) {
        currentPolyline?.remove()
        val polylineOptions = PolylineOptions()
            .addAll(path)
            .width(8f)
            .color(Color.BLUE)
        currentPolyline = mMap!!.addPolyline(polylineOptions)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}