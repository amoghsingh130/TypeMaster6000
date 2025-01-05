package com.example.typemaster6000

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class LoginActivity : AppCompatActivity() {
    private lateinit var guestButton: MaterialButton
    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            Log.d(TAG, "Starting onCreate")
            setContentView(R.layout.activity_login)

            // Initialize guest button
            guestButton = findViewById(R.id.guestButton)
            Log.d(TAG, "Found guest button")

            guestButton.setOnClickListener {
                Log.d(TAG, "Guest button clicked")
                try {
                    startMainActivity()
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting MainActivity", e)
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun startMainActivity() {
        Log.d(TAG, "Starting MainActivity")
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
} 
