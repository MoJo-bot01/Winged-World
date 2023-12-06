package com.example.wingedworld.activities.activities

import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.wingedworld.R
import com.example.wingedworld.databinding.ActivityHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

//View binding
private lateinit var binding: ActivityHomeBinding
//firebase auth
private lateinit var auth: FirebaseAuth

//firebase user
private lateinit var user: FirebaseUser

private lateinit var progressDialog: ProgressDialog


private const val LOCATION_PERMISSION_REQUEST_CODE = 1


class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //init Firebase auth
        auth = FirebaseAuth.getInstance()

        //init FIrebase user
        user = auth.currentUser!!

        //init progress dialog, will show while creating account
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)

        // Handle click on the menu button
        binding.menuBtn.setOnClickListener {
            openMenu()
        }

        binding.startBtn.setOnClickListener {
            checkLocationPermissionAndStartMap()
        }

        binding.settingsBtn.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
        }

        binding.helpBtn.setOnClickListener {
            startActivity(Intent(this, ContactUsActivity::class.java))
            finish()
        }
    }

    private fun checkLocationPermissionAndStartMap() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Location permission is granted, so start the MapActivity
            startActivity(Intent(this, MapActivity::class.java))
            finish()
        } else {
            // Location permission is not granted, request permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Location permission is granted, start the MapActivity
                startActivity(Intent(this, MapActivity::class.java))
                finish()
            } else {
                // Location permission is denied, you can handle this case as needed
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun openMenu() {
        // Implement your menu functionality here, for example, show a popup menu
        val popupMenu = PopupMenu(this@HomeActivity, binding.menuBtn)
        popupMenu.menuInflater.inflate(R.menu.whole_menu, popupMenu.menu)

        // Set menu item click listener
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.settingBtn -> {
                    showSettings()
                    true
                }

                R.id.observationsBtn -> {
                showObservationsHotspots()
                true
            }
                R.id.logoutBtn -> {
                    showLogoutConfirmationDialog()
                    true
                }
                else -> false
            }
        }

        // Show the popup menu
        popupMenu.show()
    }

    private fun showSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
        finish()
    }

    private fun showObservationsHotspots() {
        startActivity(Intent(this, BirdsObservationsActivity::class.java))
        finish()
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
}