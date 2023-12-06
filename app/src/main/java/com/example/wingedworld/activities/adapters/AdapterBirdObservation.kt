package com.example.wingedworld.activities.adapters

import android.app.AlertDialog
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.wingedworld.R
import com.example.wingedworld.activities.models.ModelBirdObservation
import com.example.wingedworld.databinding.RowBirdObservationBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.IOException
import java.util.Locale

class AdapterBirdObservation : RecyclerView.Adapter<AdapterBirdObservation.HolderBirdObs>{

    private val context: Context

    private var birdObsArrayList: ArrayList<ModelBirdObservation>

    //firebase auth
    private lateinit var auth: FirebaseAuth

    private lateinit var binding: RowBirdObservationBinding

    constructor(
        context: Context,
        birdObsArrayList: ArrayList<ModelBirdObservation>
    ) {
        this.context = context
        this.birdObsArrayList = birdObsArrayList
    }

    inner class HolderBirdObs(birdObsView: View): RecyclerView.ViewHolder(birdObsView){
        var name = binding.birdNameTextView1
        var date = binding.birdDateTextView1
        var count = binding.birdCountTextView1
        var location = binding.birdLocationTextView1
        var moreOptions = binding.moreBtn
        var userProfile = binding.profileIv
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderBirdObs {
        binding = RowBirdObservationBinding.inflate(LayoutInflater.from(context),parent,false)

        return HolderBirdObs(binding.root)
    }

    override fun getItemCount(): Int {
        return birdObsArrayList.size
    }

    override fun onBindViewHolder(holder: HolderBirdObs, position: Int) {
        val model = birdObsArrayList[position]

        val id = model.id
        val uid = model.uid
        val name = model.name
        val date = model.date
        val profileImage = model.profileImage
        val timestamp = model.timestamp
        val count = model.count
        val latitude = model.latitude
        val longitude = model.longitude
        val location = model.location

        auth = FirebaseAuth.getInstance()

        loadBirdObs(model,holder)

        holder.moreOptions.setOnClickListener {
            moreOptionDialog(model, holder)
        }
    }

    private fun moreOptionDialog(
        model: ModelBirdObservation,
        holder: HolderBirdObs
    ) {
        val id = model.id

        // Define the options array
        val options = arrayOf("Delete Bird Observation")

        // Create and show the AlertDialog
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Choose Option")
            .setItems(options) { _, _ ->
                showDeleteConfirmationDialog(id)
            }
            .show()
    }

    private fun showDeleteConfirmationDialog(id: String) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Delete Bird Observation")
            .setMessage("Are You Sure You Want To Delete This Bird Observation?")
            .setPositiveButton("Delete") { _, _ ->
                deleteBirdObs(id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteBirdObs(id: String) {
        val ref = FirebaseDatabase.getInstance().getReference("BirdObservation")
        ref.child(id).removeValue()
            .addOnSuccessListener {
                // Forum deleted successfully, you might want to update your UI here
                notifyDataSetChanged() // Refresh the RecyclerView
                Toast.makeText(context, "Bird Observation Deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                // Handle failure to delete forum
                Toast.makeText(context, "Failed To Delete Bird Observation Due To: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadBirdObs(
        model: ModelBirdObservation,
        holder: HolderBirdObs
    ) {

        //init firebase auth
        auth = FirebaseAuth.getInstance()
        val user = auth.uid

        val birdObsId = model.id

        val ref = FirebaseDatabase.getInstance().getReference("BirdObservation")
        ref.child(birdObsId)
            .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
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


                    model.id = id
                    model.uid = uid
                    model.name = name
                    model.date = date
                    model.profileImage = profileImage
                    model.count = count.toInt()
                    model.location = location

                    if (user == uid){
                        holder.name.text = name
                        holder.date.text = date
                        holder.count.text = count
                        holder.location.text = location

                        try {
                            Glide.with(context)
                                .load(profileImage)
                                .placeholder(R.drawable.person_gray)
                                .into(holder.userProfile)
                        } catch (e: Exception) {
                            // Handle Glide exception if needed
                        }
                    }

                }
                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })
    }
}