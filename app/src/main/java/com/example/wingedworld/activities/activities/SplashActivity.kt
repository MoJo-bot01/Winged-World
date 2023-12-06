package com.example.wingedworld.activities.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.example.wingedworld.databinding.ActivitySplashBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

//view binding
private lateinit var binding: ActivitySplashBinding

//firebase auth
private lateinit var auth: FirebaseAuth


class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //init firebase auth
        auth = FirebaseAuth.getInstance()

        Handler().postDelayed(Runnable{
            checkUser()
        }, 3000)//means 3 seconds
    }

    private fun checkUser() {
        val firebaseUser = auth.currentUser
        if (firebaseUser == null){
            startActivity(Intent(this, LaunchActivity::class.java))
            finish()
        }
        else{

            val ref = FirebaseDatabase.getInstance().getReference("Users")
            ref.child(firebaseUser.uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {

                        when (snapshot.child("userType").value) {
                            "user" -> {
                                startActivity(Intent(this@SplashActivity, HomeActivity::class.java))
                                finish()
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {

                    }

                })
        }
    }
}