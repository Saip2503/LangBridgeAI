package com.example.langbridgai

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task

class LoginActivity : AppCompatActivity() {

    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonLogin: Button
    private lateinit var buttonGoogleSignIn: SignInButton // Declare Google Sign-In Button
    private lateinit var sharedViewModel: SharedViewModel

    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private val SIGN_IN = 9001 // Request code for Google Sign-In

    // ActivityResultLauncher for Google Sign-In
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        sharedViewModel = ViewModelProvider(this).get(SharedViewModel::class.java)

        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPassword = findViewById(R.id.editTextPassword)
        buttonLogin = findViewById(R.id.buttonLogin)
        buttonGoogleSignIn = findViewById(R.id.button_google_sign_in) // Initialize Google Sign-In button

        // Configure Google Sign-In to request the user's ID, email, and basic profile.
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            // Use the web client ID (from strings.xml) to request ID token for backend verification
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .requestProfile()
            .build()

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        // Initialize the ActivityResultLauncher for Google Sign-In
        googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                handleSignInResult(task)
            } else {
                Toast.makeText(this, "Google Sign-In cancelled or failed.", Toast.LENGTH_SHORT).show()
                Log.e("GoogleSignIn", "Google Sign-In failed with result code: ${result.resultCode}")
            }
        }


        buttonLogin.setOnClickListener {
            performLogin()
        }

        buttonGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }

        // Auto-fill for testing direct login
        editTextEmail.setText("demo@demo.com")
        editTextPassword.setText("demo")
    }

    override fun onStart() {
        super.onStart()
        // Check for existing Google Sign-In account
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null) {
            // User is already signed in with Google
            updateUIWithGoogleAccount(account)
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = mGoogleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent) // Use the new launcher
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            // Signed in successfully, show authenticated UI.
            updateUIWithGoogleAccount(account)
        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w("GoogleSignIn", "signInResult:failed code=" + e.statusCode)
            Toast.makeText(this, "Google Sign-In failed. Code: ${e.statusCode}", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateUIWithGoogleAccount(account: GoogleSignInAccount) {
        val displayName = account.displayName ?: account.email ?: "Google User"
        val email = account.email ?: "N/A"
        val idToken = account.idToken // This is the ID token to send to your backend

        Toast.makeText(this, "Signed in as: $displayName", Toast.LENGTH_LONG).show()
        Log.d("GoogleSignIn", "ID Token: $idToken") // Log the ID token for debugging backend integration

        // Store user info in SharedViewModel
        sharedViewModel.setUserName(displayName)
        sharedViewModel.setUserEmail(email) // You'll need to add setUserEmail to SharedViewModel

        // Proceed to MainActivity
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
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
            // For demo login, set a placeholder username
            sharedViewModel.setUserName(email)
            sharedViewModel.setUserEmail(email) // For demo login, email is the same

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "Invalid credentials. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }
}
