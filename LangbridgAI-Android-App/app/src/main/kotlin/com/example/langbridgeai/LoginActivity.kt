package com.example.langbridgai

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonLogin: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login) // Set the content view to your login layout

        // Initialize UI elements
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPassword = findViewById(R.id.editTextPassword)
        buttonLogin = findViewById(R.id.buttonLogin)

        // Set up the login button click listener
        buttonLogin.setOnClickListener {
            performLogin()
        }

        // Optional: Pre-fill demo credentials for quick testing (as shown in your image)
        editTextEmail.setText("demo@demo.com")
        editTextPassword.setText("demo")
    }

    private fun performLogin() {
        val email = editTextEmail.text.toString().trim()
        val password = editTextPassword.text.toString().trim()

        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
            editTextEmail.error = "Email is required"
            editTextEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show()
            editTextPassword.error = "Password is required"
            editTextPassword.requestFocus()
            return
        }

        // --- AUTHENTICATION LOGIC GOES HERE ---
        // For a real application, you would integrate with Firebase Authentication
        // or your custom backend API here.

        // Example Placeholder Authentication (DO NOT USE IN PRODUCTION)
        if (email == "demo@demo.com" && password == "demo") {
            Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()
            // Navigate to the main activity or dashboard after successful login
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Close LoginActivity so user can't go back with back button
        } else {
            Toast.makeText(this, "Invalid credentials. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }
}
