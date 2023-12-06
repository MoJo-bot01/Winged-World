package com.example.wingedworld.activities.activities

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.wingedworld.R
import com.example.wingedworld.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

//view binding
private lateinit var binding: ActivityLoginBinding

private lateinit var client: GoogleSignInClient

//firebase auth
private lateinit var auth: FirebaseAuth


//progress dialog
private lateinit var progressDialog: ProgressDialog



class LoginActivity : AppCompatActivity() {

    companion object {
        private const val RC_SIGN_IN = 9001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //init firebase auth
        auth = FirebaseAuth.getInstance()

        //init progress dialog, will show while creating account
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)

        binding.backBtn.setOnClickListener {
            val intent = Intent(this, LaunchActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.noAccountTv.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        binding.forgotTv.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        binding.loginBtn.setOnClickListener {
            validateData()
        }

//        //Handle click sign in with google
        binding.signInGoogle.setOnClickListener {
            progressDialog.setMessage("Logging In...")
            progressDialog.show()
            val intent = client.signInIntent
            startActivityForResult(intent,10001)

            // Delay the dismissal of the progressDialog for 3 seconds
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({
                progressDialog.dismiss()
            }, 3000) // 3000 milliseconds = 3 seconds
        }

        binding.signInGoogle.setOnClickListener {
            signIn()
        }
    }

    private fun signIn() {
        progressDialog.setMessage("Logging In...")
        progressDialog.show()
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, options)
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)

        // Delay the dismissal of the progressDialog for 3 seconds
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            progressDialog.dismiss()
        }, 3000) // 3000 milliseconds = 3 seconds
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Toast.makeText(this, "Signed in as ${user?.displayName}", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    //local variables
    private var email1 = " "
    private var password1 = " "

    private fun validateData() {

        email1 = binding.emailEt.text.toString().trim()
        password1 = binding.passwordEt.text.toString().trim()

        if (!Patterns.EMAIL_ADDRESS.matcher(email1).matches()) {
            Toast.makeText(baseContext, "Invalid Email Address...", Toast.LENGTH_SHORT).show()
        } else if (TextUtils.isEmpty(password1)) {
            Toast.makeText(baseContext, "Enter Password...", Toast.LENGTH_SHORT).show()
        } else {
            loginUser()
        }
    }

    private fun loginUser() {

        progressDialog.setMessage("Logging In...")
        progressDialog.show()

        auth.signInWithEmailAndPassword(email1, password1)
            .addOnSuccessListener {
                progressDialog.dismiss()
                checkUser()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(baseContext, "Login Failed Due To ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
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
                                startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
                                finish()
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {

                    }

                })
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}