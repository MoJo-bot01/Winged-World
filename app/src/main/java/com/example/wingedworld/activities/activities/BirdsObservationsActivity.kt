package com.example.wingedworld.activities.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.wingedworld.activities.adapters.AdapterBirdObservation
import com.example.wingedworld.activities.models.ModelBirdObservation
import com.example.wingedworld.databinding.ActivityBirdsObservationsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

//View binding
private lateinit var binding: ActivityBirdsObservationsBinding

//firebase user
private lateinit var user: FirebaseUser

//firebase auth
private lateinit var auth: FirebaseAuth

private lateinit var birdObsArrayList: ArrayList<ModelBirdObservation>

private lateinit var adapterBirdObs: AdapterBirdObservation

class BirdsObservationsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBirdsObservationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //init firebase auth
        auth = FirebaseAuth.getInstance()
        user = auth.currentUser!!

        // Get the current user's ID
        val currentUserId = user.uid

        loadBirdsObservations(currentUserId)

        binding.backBtn.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        binding.addNewBirdBtn.setOnClickListener {
            startActivity(Intent(this, AddNewBirdObservationActivity::class.java))
            finish()
        }
    }

    private fun loadBirdsObservations(userId: String) {
        birdObsArrayList = ArrayList()

        val ref = FirebaseDatabase.getInstance().getReference("BirdObservation")
        ref.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                birdObsArrayList.clear()
                for (ds in snapshot.children) {
                    val birdObsId = "${ds.child("id").value}"
                    val uid = "${ds.child("uid").value}"

                    // Check if the observation belongs to the current user
                    if (uid == userId) {
                        val modelBirdObs = ModelBirdObservation()
                        modelBirdObs.id = birdObsId

                        birdObsArrayList.add(modelBirdObs)
                    }
                }

                adapterBirdObs = AdapterBirdObservation(
                    this@BirdsObservationsActivity,
                    birdObsArrayList
                )

                binding.favoriteRv.adapter = adapterBirdObs
                binding.favoriteItemsCountTv.text = birdObsArrayList.size.toString()
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}