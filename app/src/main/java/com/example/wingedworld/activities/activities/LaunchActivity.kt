package com.example.wingedworld.activities.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.wingedworld.databinding.ActivityLaunchBinding

//view binding
private lateinit var binding: ActivityLaunchBinding


class LaunchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLaunchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Handle click login
        binding.loginBtn.setOnClickListener {
            //move to login page
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        //Handle click sign up
        binding.signupBtn.setOnClickListener {
            //move to login page
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}