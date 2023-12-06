package com.example.wingedworld.activities.activities

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.wingedworld.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

//view binding
private lateinit var binding: ActivitySignUpBinding

//firebase auth
private lateinit var auth: FirebaseAuth

//progress dialog
private lateinit var progressDialog: ProgressDialog


class SignUpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //init firebase auth
        auth = FirebaseAuth.getInstance()

        //init progress dialog, will show while creating account
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)

        //handle back button
        binding.backBtn.setOnClickListener {
            val intent = Intent(this, LaunchActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.yesAccountTv.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        //handle sign up button
        binding.signupBtn.setOnClickListener {
            validateData()
        }
    }

    //local variables
    private var name1 = " "
    private var email1 = " "
    private var phoneNumber = " "
    private var password1 = " "
    private var confirmPassword1 = " "

    private fun validateData() {
        name1 = binding.nameEt.text.toString().trim()
        email1 = binding.emailEt.text.toString().trim()
        phoneNumber = binding.contactEt.text.toString().trim()
        password1 = binding.passwordEt.text.toString().trim()
        confirmPassword1 = binding.cPasswordEt.text.toString().trim()

        val phonePattern = "^(\\+27|0)[6-8][0-9]{8}$"

        if (TextUtils.isEmpty(name1)) {
            Toast.makeText(baseContext, "Enter Your Name...", Toast.LENGTH_SHORT).show()
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email1).matches()) {
            Toast.makeText(baseContext, "Invalid Email Address...", Toast.LENGTH_SHORT).show()
        } else if (TextUtils.isEmpty(phoneNumber)) {
            Toast.makeText(baseContext, "Enter Phone Number...", Toast.LENGTH_SHORT).show()
        } else if (!phoneNumber.matches(phonePattern.toRegex())) {
            Toast.makeText(
                baseContext,
                "Enter a valid South African cellphone number...",
                Toast.LENGTH_SHORT
            ).show()
        } else if (TextUtils.isEmpty(password1)) {
            Toast.makeText(baseContext, "Enter Password...", Toast.LENGTH_SHORT).show()
        } else if (TextUtils.isEmpty(confirmPassword1)) {
            Toast.makeText(baseContext, "Confirm Password...", Toast.LENGTH_SHORT).show()
        } else if (password1 != confirmPassword1) {
            Toast.makeText(baseContext, "Password Does Not Match...", Toast.LENGTH_SHORT).show()
        } else {
            createUserAccount()
        }
    }

    private fun createUserAccount() {

        progressDialog.setMessage("Creating Account...")
        progressDialog.show()

        auth.createUserWithEmailAndPassword(email1,password1)
            .addOnSuccessListener {
                progressDialog.dismiss()
                // Send verification email to the user
                val user = auth.currentUser
                user?.sendEmailVerification()
                    ?.addOnSuccessListener {
                        Toast.makeText(
                            baseContext,
                            "Verification email sent. Please check your inbox.",
                            Toast.LENGTH_SHORT
                        ).show()
                        updateUserInfo()
                    }
                    ?.addOnFailureListener { e ->

                    }
            }
            .addOnFailureListener { e->
                progressDialog.dismiss()
                Toast.makeText(
                    baseContext,
                    "Failed Creating An Account Due To ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun updateUserInfo() {

        progressDialog.setMessage("Creating Account...")
        progressDialog.show()
        val timestamp = System.currentTimeMillis()

        val uid = auth.uid

        val hashMap: HashMap<String, Any?> = HashMap()
        hashMap["id"] = "$timestamp"
        hashMap["uid"] = uid
        hashMap["name"] = name1
        hashMap["email"] = email1
        hashMap["password"] = password1
        hashMap["phoneNumber"] = phoneNumber
        hashMap["profileImage"] = ""
        hashMap["userType"] = "user"
        hashMap["timestamp"] = timestamp

        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(uid!!)
            .setValue(hashMap)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(baseContext, "Account Created...", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@SignUpActivity, LoginActivity::class.java))
                finish()
            }
            .addOnFailureListener { e->
                progressDialog.dismiss()
                Toast.makeText(
                    baseContext,
                    "Failed To Create Account Due To ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}