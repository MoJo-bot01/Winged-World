package com.example.wingedworld.activities.activities

import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.view.Menu
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.wingedworld.R
import com.example.wingedworld.databinding.ActivityEditProfileUserBinding
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

//firebase user
private lateinit var user: FirebaseUser

//firebase auth
private lateinit var auth: FirebaseAuth

private lateinit var progressDialog: ProgressDialog

//Image uri
private var imageUri: Uri? = null

//View binding
private lateinit var binding: ActivityEditProfileUserBinding


class EditProfileUserActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //init firebase auth
        auth = FirebaseAuth.getInstance()
        user = auth.currentUser!!

        loadUserInfo()

        //Setup progress dialog
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait...")
        progressDialog.setCanceledOnTouchOutside(false)

        //Handle click, back image button
        binding.backBtn.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
        }

        //Handle click, pick image from camera/gallery
        binding.profileIv.setOnClickListener {
            showImageAttachMenu()
        }

        //Handle click, begin update profile
        binding.updateBtn.setOnClickListener {
            validateData()
        }
    }

    private var name =""
    private var phoneNumber =""
    private fun validateData() {
        //Get data
        name = binding.nameEt.text.toString().trim()
        phoneNumber = binding.contactEt.text.toString().trim()

        val phonePattern = "^(\\+27|0)[6-8][0-9]{8}$"

        //Validate data
        if (name.isEmpty()){
            Toast.makeText(this, "Enter name", Toast.LENGTH_SHORT).show()
        }else if (TextUtils.isEmpty(phoneNumber)) {
            Toast.makeText(baseContext, "Enter Phone Number...", Toast.LENGTH_SHORT).show()
        } else if (!phoneNumber.matches(phonePattern.toRegex())) {
            Toast.makeText(
                baseContext,
                "Enter a valid South African cellphone number...",
                Toast.LENGTH_SHORT
            ).show()
        }
        else{
            if (imageUri == null){
                updateProfile("")
            }
            else{
                uploadImage()
            }
        }
    }

    private fun uploadImage() {
        progressDialog.setMessage("Uploading profile image")
        progressDialog.show()

        val filePathAndName = "ProfileImages/"+ auth.uid

        val ref = FirebaseStorage.getInstance().getReference(filePathAndName)
        ref.putFile(imageUri!!)
            .addOnSuccessListener { taskSnapshot->
                progressDialog.dismiss()
                val uriTask: Task<Uri> = taskSnapshot.storage.downloadUrl
                while(!uriTask.isSuccessful);
                val uploadedImageUrl = "${uriTask.result}"

                updateProfile(uploadedImageUrl)
            }

    }

    private fun updateProfile(uploadedImageUrl: String) {
        progressDialog.setMessage("Updating profile")
        progressDialog.show()

        //Setup info to update to db
        val hashmap: HashMap<String, Any> = HashMap()
        hashmap["name"] = "$name"
        hashmap["phoneNumber"] = "$phoneNumber"
        if (imageUri != null){
            hashmap["profileImage"] = uploadedImageUrl
        }

        //update db
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(auth.uid!!)
            .updateChildren(hashmap)
            .addOnSuccessListener {
                //Profile updated
                progressDialog.dismiss()
                Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, SettingsActivity::class.java))
                finish()
            }
            .addOnFailureListener {e ->
                //Failed to upload image
                progressDialog.dismiss()
                Toast.makeText(
                    this,
                    "Failed to update image due to ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun loadUserInfo() {
        //Load user info
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(auth.uid!!)
            .addValueEventListener(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    //Get user info
                    val name = "${snapshot.child("name").value}"
                    val phoneNumber = "${snapshot.child("phoneNumber").value}"
                    val profileImage = "${snapshot.child("profileImage").value}"
                    val timestamp = "${snapshot.child("timestamp").value}"

                    binding.nameEt.setText(name)
                    binding.contactEt.setText(phoneNumber)

                    try {
                        Glide.with(this@EditProfileUserActivity)
                            .load(profileImage)
                            .placeholder(R.drawable.person_gray)
                            .into(binding.profileIv)
                    }
                    catch (e: Exception){

                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })
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