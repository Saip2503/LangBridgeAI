package com.example.langbridgai

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider // Import ViewModelProvider
import com.example.langbridgeai.SharedViewModel

class LoginActivity : AppCompatActivity() {

    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonLogin: Button
    private lateinit var sharedViewModel: SharedViewModel // Declare SharedViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize SharedViewModel
        sharedViewModel = ViewModelProvider(this).get(SharedViewModel::class.java)

        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPassword = findViewById(R.id.editTextPassword)
        buttonLogin = findViewById(R.id.buttonLogin)

        buttonLogin.setOnClickListener {
            performLogin()
        }

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

        if (email == "demo@demo.com" && password == "demo") {
            Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()
            // Set the username in the SharedViewModel after successful login
            sharedViewModel.setUserName(email)

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "Invalid credentials. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }
}
