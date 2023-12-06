package com.example.wingedworld.activities.activities

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.wingedworld.R
import com.example.wingedworld.databinding.ActivityContactUsBinding
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase

//firebase user
private lateinit var user: FirebaseUser

//firebase auth
private lateinit var auth: FirebaseAuth

//View binding
private lateinit var binding: ActivityContactUsBinding

private lateinit var progressDialog: ProgressDialog


class ContactUsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactUsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        user = auth.currentUser!!

        //Setup progress dialog
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait...")
        progressDialog.setCanceledOnTouchOutside(false)

        binding.backBtn.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
        }

        binding.sendChatBtn.setOnClickListener {
            validateData()
        }
    }

    //local variables
    private var message = " "

    private fun validateData() {

        message = binding.messageEt.text.toString().trim()

        if (TextUtils.isEmpty(message)) {
            Toast.makeText(baseContext, "Enter Message...", Toast.LENGTH_SHORT).show()
        }  else {
            showConfirmEmailDialog()
        }
    }

    private fun showConfirmEmailDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_confirm_email, null)
        val yesButton = dialogView.findViewById<Button>(R.id.yesButton)
        val noButton = dialogView.findViewById<Button>(R.id.noButton)
        val email = dialogView.findViewById<TextView>(R.id.emailMessage)
        val close = dialogView.findViewById<ImageButton>(R.id.closeBtn)

        email.text = user.email

        val alertDialogBuilder = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)

        val alertDialog = alertDialogBuilder.create()


        yesButton.setOnClickListener {
            alertDialog.dismiss()
            showMessageSentDialog()
        }
        noButton.setOnClickListener {
            alertDialog.dismiss()
            showEnterNewEmailDialog()
        }
        close.setOnClickListener {
            alertDialog.dismiss()
        }
        alertDialog.show()
    }

    private fun showEnterNewEmailDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_enter_new_email, null)
        val nextButton = dialogView.findViewById<Button>(R.id.nextButton)
        val backButton = dialogView.findViewById<Button>(R.id.backButton)
        val emailEt1 = dialogView.findViewById<EditText>(R.id.emailEt1)

        val alertDialogBuilder = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)

        val alertDialog = alertDialogBuilder.create()

        nextButton.setOnClickListener {
            val newEmail = emailEt1.text.toString()
            if (!Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                Toast.makeText(baseContext, "Invalid Email Address...", Toast.LENGTH_SHORT).show()
            } else {
                alertDialog.dismiss()
                showChangeEmailDialog(newEmail)
            }
        }

        backButton.setOnClickListener {
            alertDialog.dismiss()
            showConfirmEmailDialog()
        }

        alertDialog.show()
    }


    private fun showChangeEmailDialog(emailAddress: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_email, null)
        val yesButton = dialogView.findViewById<Button>(R.id.yesButton)
        val noButton = dialogView.findViewById<Button>(R.id.noButton)
        val newEmail = dialogView.findViewById<TextView>(R.id.newEmailMessage)
        val newEmail1 = dialogView.findViewById<TextView>(R.id.emailMessage)

        newEmail1.text = user.email
        newEmail.text = emailAddress

        val alertDialogBuilder = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)

        val alertDialog = alertDialogBuilder.create()

        yesButton.setOnClickListener {
            alertDialog.dismiss()
            showEnterYourPassword(emailAddress)
        }
        noButton.setOnClickListener {
            alertDialog.dismiss()
            showMessageSent1Dialog()
        }
        alertDialog.show()
    }

    private fun showEnterYourPassword(emailAddress: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_enter_password, null)
        val confirmPasswordButton = dialogView.findViewById<Button>(R.id.confirmPasswordButton)
        val logoutButton = dialogView.findViewById<Button>(R.id.cancelButton)
        val passwordEt = dialogView.findViewById<EditText>(R.id.passwordEt1)

        val alertDialogBuilder = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)

        val alertDialog = alertDialogBuilder.create()

        confirmPasswordButton.setOnClickListener {
            val password = passwordEt.text.toString()
            if (TextUtils.isEmpty(password)) {
                Toast.makeText(baseContext, "Enter Your Current Login Password...", Toast.LENGTH_SHORT).show()
            } else {
                alertDialog.dismiss()
                changeUserAuthEmail(emailAddress,password)
            }
        }

        logoutButton.setOnClickListener {
            alertDialog.dismiss()
            logoutOfApp()
        }

        alertDialog.show()
    }

    private fun logoutOfApp() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_force_logout, null)
        val okButton = dialogView.findViewById<Button>(R.id.okButton)
        val logoutReason = dialogView.findViewById<TextView>(R.id.logoutMessage)

        logoutReason.text = "Incorrect Password"

        val alertDialogBuilder = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)

        val alertDialog = alertDialogBuilder.create()

        okButton.setOnClickListener {
            alertDialog.dismiss()
            auth.signOut()
            startActivity(Intent(this, SplashActivity::class.java))
            finish()
        }
        alertDialog.show()
    }

    private fun showMessageSent1Dialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_message_sent1, null)
        val okButton = dialogView.findViewById<Button>(R.id.okButton)

        val alertDialogBuilder = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)

        val alertDialog = alertDialogBuilder.create()

        okButton.setOnClickListener {
            alertDialog.dismiss()
            binding.messageEt.text.clear()
        }
        alertDialog.show()
    }

    private fun showEmailChangedAndMessageSentDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_email_changed_and_message_sent, null)
        val okButton = dialogView.findViewById<Button>(R.id.okButton)

        val alertDialogBuilder = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)

        val alertDialog = alertDialogBuilder.create()

        okButton.setOnClickListener {
            alertDialog.dismiss()
            binding.messageEt.text.clear()
        }
        alertDialog.show()
    }

    private fun changeUserAuthEmail(email: String, password1: String) {
        val user = FirebaseAuth.getInstance().currentUser

// Check if the user is signed in
        if (user != null) {
            // Re-authenticate the user (You might need to adjust this based on your authentication method)
            val credential = EmailAuthProvider.getCredential(user.email!!, password1)
            user.reauthenticate(credential)
                .addOnCompleteListener { reauthResult ->
                    if (reauthResult.isSuccessful) {
                        // Update the user's email
                        user.updateEmail(email)
                            .addOnCompleteListener { updateEmailResult ->
                                if (updateEmailResult.isSuccessful) {
                                    // Email updated successfully
                                    saveNewEmailToProfile(email)
                                } else {
                                    // Handle the error
                                    // Handle the error
                                    val dialogView = layoutInflater.inflate(R.layout.dialog_force_logout, null)
                                    val okButton = dialogView.findViewById<Button>(R.id.okButton)
                                    val logoutReason = dialogView.findViewById<Button>(R.id.logoutMessage)

                                    logoutReason.text = "Cancelling Account Ownership Verification."

                                    val alertDialogBuilder = android.app.AlertDialog.Builder(this)
                                        .setView(dialogView)
                                        .setCancelable(false)

                                    val alertDialog = alertDialogBuilder.create()

                                    okButton.setOnClickListener {
                                        alertDialog.dismiss()
                                        startActivity(Intent(this, SplashActivity::class.java))
                                        finish()
                                    }
                                    alertDialog.show()
                                }
                            }
                    } else {
                        // Handle the error
                        val dialogView = layoutInflater.inflate(R.layout.dialog_force_logout, null)
                        val okButton = dialogView.findViewById<Button>(R.id.okButton)
                        val logoutReason = dialogView.findViewById<TextView>(R.id.logoutMessage)

                        logoutReason.text = "Incorrect Password"

                        val alertDialogBuilder = android.app.AlertDialog.Builder(this)
                            .setView(dialogView)
                            .setCancelable(false)

                        val alertDialog = alertDialogBuilder.create()

                        okButton.setOnClickListener {
                            alertDialog.dismiss()
                            startActivity(Intent(this, SplashActivity::class.java))
                            finish()
                        }
                        alertDialog.show()
                    }
                }
        }

    }

    private fun saveNewEmailToProfile(email: String) {
        progressDialog.setMessage("Saving Your New Email")
        progressDialog.show()

        //Setup info to update to db
        val hashmap: HashMap<String, Any> = HashMap()
        hashmap["email"] = "$email"

        //update db
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(auth.uid!!)
            .updateChildren(hashmap)
            .addOnSuccessListener {
                //Profile updated
                progressDialog.dismiss()
                Toast.makeText(this, "Email Updated", Toast.LENGTH_SHORT).show()
                showEmailChangedAndMessageSentDialog()
            }
            .addOnFailureListener {e ->
                //Failed to upload image
                progressDialog.dismiss()
                Toast.makeText(
                    this,
                    "Failed To Update Email Due To ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun showMessageSentDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_message_sent2, null)
        val okButton = dialogView.findViewById<Button>(R.id.okButton)

        val alertDialogBuilder = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)

        val alertDialog = alertDialogBuilder.create()

        okButton.setOnClickListener {
            alertDialog.dismiss()
            binding.messageEt.text.clear()
        }
        alertDialog.show()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}