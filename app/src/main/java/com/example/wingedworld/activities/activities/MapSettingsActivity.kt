package com.example.wingedworld.activities.activities

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.wingedworld.R
import com.example.wingedworld.databinding.ActivityMapSettingsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Locale

//Progress dialog
private lateinit var progressDialog: ProgressDialog

//View binding
private lateinit var binding: ActivityMapSettingsBinding

//firebase auth
private lateinit var auth: FirebaseAuth

//firebase user
private lateinit var user: FirebaseUser


class MapSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //init firebase auth
        auth = FirebaseAuth.getInstance()

        //init firebase user
        user = auth.currentUser!!

        loadPreferences()

        //init progress dialog, will show while creating account
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)

        binding.backBtn.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
            finish()
        }

        binding.resetBtn.setOnClickListener {
            resetMapSettings()
        }

        binding.applyBtn.setOnClickListener {
            val checkedRadioButtonId = binding.radioGroupDistanceUnit.checkedRadioButtonId

            if (checkedRadioButtonId == R.id.radioButtonKilometers) {
                distanceUnit = "km"
            } else if (checkedRadioButtonId == R.id.radioButtonMiles) {
                distanceUnit = "ml"
            }
            validateData()
        }
    }

    private fun loadPreferences(){
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
                        if (distanceNum != null && distanceUnit != null){
                            binding.distanceEt.setText(distanceNum)

                            if (distanceUnit == "ml"){
                                binding.radioButtonMiles.isChecked = true
                            }
                            else if (distanceUnit == "km") {
                                binding.radioButtonKilometers.isChecked = true
                            }
                        }
                    }
                    else{

                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@MapSettingsActivity,
                    "Error loading map settings due to $error",
                    Toast.LENGTH_SHORT
                ).show()
            }

        })
    }

    private fun resetMapSettings() {
        progressDialog.setMessage("Resetting Settings...")
        progressDialog.show()

        val uid = auth.uid
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val timestamp = System.currentTimeMillis()
        val email = auth.currentUser?.email

        // Setup data to add in db
        val hashMap: HashMap<String, Any?> = HashMap()
        hashMap["id"] = "$timestamp"
        hashMap["uid"] = uid
        hashMap["distanceUnit"] = "km"
        hashMap["distanceNum"] = "0"
        hashMap["timestamp"] = timestamp
        Log.d("DistanceValue", "Value: $distanceNum")

        // Save to db
        val ref = FirebaseDatabase.getInstance().getReference("MapSettings")
        ref.child(uid!!)
            .setValue(hashMap)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Map Settings Back To Default...", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MapActivity::class.java)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e->
                progressDialog.dismiss()
                Toast.makeText(
                    this,
                    "Failed To Reset Map Settings Due To ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private var distanceUnit = " "
    private var distanceNum = ""

    private fun validateData() {
        distanceNum = binding.distanceEt.text.toString().trim()

        if (TextUtils.isEmpty(distanceNum)) {
            Toast.makeText(
                baseContext,
                "Enter the distance you are willing to travel...",
                Toast.LENGTH_SHORT
            ).show()
        }
        else{
            applyMapSettings()
        }
    }

    private fun applyMapSettings() {
        progressDialog.setMessage("Applying settings...")
        progressDialog.show()

        val uid = auth.uid
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val timestamp = System.currentTimeMillis()
        val email = auth.currentUser?.email

        // Setup data to add in db
        val hashMap: HashMap<String, Any?> = HashMap()
        hashMap["id"] = "$timestamp"
        hashMap["uid"] = uid
        hashMap["distanceUnit"] = distanceUnit
        hashMap["distanceNum"] = distanceNum
        hashMap["timestamp"] = timestamp
        Log.d("DistanceValue", "Value: $distanceNum")

        // Save to db
        val ref = FirebaseDatabase.getInstance().getReference("MapSettings")
        ref.child(uid!!)
            .setValue(hashMap)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Map settings applied", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MapActivity::class.java)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e->
                progressDialog.dismiss()
                Toast.makeText(
                    this,
                    "Failed to apply map settings applied due to ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}