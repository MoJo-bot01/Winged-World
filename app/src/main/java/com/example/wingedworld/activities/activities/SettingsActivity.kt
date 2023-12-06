package com.example.wingedworld.activities.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.bumptech.glide.Glide
import com.example.wingedworld.R
import com.example.wingedworld.databinding.ActivitySettingsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

//Firebase user
private lateinit var user: FirebaseUser

//firebase auth
private lateinit var auth: FirebaseAuth

//View binding
private lateinit var binding: ActivitySettingsBinding

private var sharedPreferences: SharedPreferences?=null

private var editor: SharedPreferences.Editor?=null

private var nightMode: Boolean = false

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize shared preferences
        sharedPreferences = getSharedPreferences("MODE", MODE_PRIVATE)
        editor = sharedPreferences?.edit()
        nightMode = sharedPreferences?.getBoolean("night", false)!!

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        user = auth.currentUser!!

        loadUserInfo()

        val modeSwitch = binding.setMode

        if (nightMode) {
            modeSwitch.isChecked = true
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            binding.themeSettTv.text = "Dark"
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            binding.themeSettTv.text = "Light"
        }

        modeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                binding.themeSettTv.text = "Dark"
                editor?.putBoolean("night", true)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                binding.themeSettTv.text = "Light"
                editor?.putBoolean("night", false)
            }
            editor?.apply()
        }

        binding.backBtn.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        binding.editProfileBtn.setOnClickListener {
            startActivity(Intent(this, EditProfileUserActivity::class.java))
            finish()
        }

        binding.helpRl.setOnClickListener {
            startActivity(Intent(this, PrivacyPolicyActivity::class.java))
            finish()
        }

        binding.contactRl.setOnClickListener {
            startActivity(Intent(this, ContactUsActivity::class.java))
            finish()
        }

        binding.logoutRl.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun showLogoutConfirmationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_confirm_logout, null)
        val confirmButton = dialogView.findViewById<Button>(R.id.confirmButton)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)

        val alertDialogBuilder = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)

        val alertDialog = alertDialogBuilder.create()

        confirmButton.setOnClickListener {
            // Handle logout action here
            auth.signOut()
            alertDialog.dismiss()
            startActivity(Intent(this, SplashActivity::class.java))
            finish()

        }
        cancelButton.setOnClickListener {
            alertDialog.dismiss()
        }
        alertDialog.show()
    }

    private fun loadUserInfo() {

        //Load user info
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(auth.uid!!)
            .addValueEventListener(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    //Get user info
                    val email = "${snapshot.child("email").value}"
                    val name = "${snapshot.child("name").value}"
                    val profileImage = "${snapshot.child("profileImage").value}"
                    val timestamp = "${snapshot.child("timestamp").value}"
                    val uid = "${snapshot.child("uid").value}"
                    val userType = "${snapshot.child("userType").value}"


                    binding.nameTv.text = name
                    binding.emailTv.text = email

                    try {
                        Glide.with(this@SettingsActivity)
                            .load(profileImage)
                            .placeholder(R.drawable.person_gray)
                            .into(binding.profileView)
                    }
                    catch (e: Exception){

                    }
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