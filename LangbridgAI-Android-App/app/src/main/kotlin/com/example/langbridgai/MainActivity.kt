package com.example.langbridgai

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.langbridgai.fragments.AccountFragment
import com.example.langbridgai.fragments.ImageTranslateFragment
import com.example.langbridgai.fragments.SpeechTranslateFragment
import com.example.langbridgai.fragments.TextTranslateFragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var fromLanguageSpinner: Spinner
    private lateinit var toLanguageSpinner: Spinner

    private lateinit var mGoogleSignInClient: GoogleSignInClient // Declare GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedViewModel = ViewModelProvider(this)[SharedViewModel::class.java]

        // Configure Google Sign-In for logout purposes
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        fromLanguageSpinner = findViewById(R.id.spinner_global_from_language)
        toLanguageSpinner = findViewById(R.id.spinner_global_to_language)

        fromLanguageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedLangName = parent?.getItemAtPosition(position).toString()
                val selectedLangCode = getLanguageCode(selectedLangName)
                sharedViewModel.setFromLanguage(selectedLangCode)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }

        toLanguageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedLangName = parent?.getItemAtPosition(position).toString()
                val selectedLangCode = getLanguageCode(selectedLangName)
                sharedViewModel.setToLanguage(selectedLangCode)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }

        val languagesArray = resources.getStringArray(R.array.languages_array)
        val defaultFromLangPosition = languagesArray.indexOf("English")
        val defaultToLangPosition = languagesArray.indexOf("Korean")

        if (defaultFromLangPosition != -1) {
            fromLanguageSpinner.setSelection(defaultFromLangPosition)
        }
        if (defaultToLangPosition != -1) {
            toLanguageSpinner.setSelection(defaultToLangPosition)
        }

        bottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.setOnItemSelectedListener { item ->
            handleBottomNavigation(item)
        }

        if (savedInstanceState == null) {
            bottomNavigationView.selectedItemId = R.id.nav_text_translate
        }
    }

    private fun handleBottomNavigation(item: MenuItem): Boolean {
        val selectedFragment: Fragment = when (item.itemId) {
            R.id.nav_text_translate -> TextTranslateFragment()
            R.id.nav_speech_translate -> SpeechTranslateFragment()
            R.id.nav_image_translate -> ImageTranslateFragment()
            R.id.nav_account -> AccountFragment()
            else -> throw IllegalArgumentException("Invalid navigation item ID")
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, selectedFragment)
            .commit()
        return true
    }

    // Function to handle logout from AccountFragment
    fun signOutAndNavigateToLogin() {
        // Sign out from Google
        mGoogleSignInClient.signOut().addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Signed out from Google.", Toast.LENGTH_SHORT).show()
                // Clear any other session data (e.g., from dummy login)
                sharedViewModel.setUserName(null)
                sharedViewModel.setUserEmail(null) // Clear email too
            } else {
                Toast.makeText(this, "Failed to sign out from Google.", Toast.LENGTH_SHORT).show()
            }
            // Navigate back to LoginActivity regardless of Google Sign-Out success
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish() // Finish MainActivity
        }
    }


    private fun getLanguageCode(languageName: String): String {
        return when (languageName) {
            "Auto Detect" -> "auto"
            "English" -> "en"
            "Korean" -> "ko"
            "Spanish" -> "es"
            "French" -> "fr"
            "German" -> "de"
            "Chinese (Simplified)" -> "zh"
            "Japanese" -> "ja"
            "Arabic" -> "ar"
            "Russian" -> "ru"
            "Portuguese" -> "pt"
            "Italian" -> "it"
            "Dutch" -> "nl"
            "Swedish" -> "sv"
            "Norwegian" -> "no"
            "Danish" -> "da"
            "Finnish" -> "fi"
            "Greek" -> "el"
            "Hebrew" -> "he"
            "Hindi" -> "hi"
            "Indonesian" -> "id"
            "Malay" -> "ms"
            "Thai" -> "th"
            "Turkish" -> "tr"
            "Vietnamese" -> "vi"
            "Polish" -> "pl"
            "Ukrainian" -> "uk"
            "Romanian" -> "ro"
            "Hungarian" -> "hu"
            "Czech" -> "cs"
            "Slovak" -> "sk"
            "Bulgarian" -> "bg"
            "Croatian" -> "hr"
            "Serbian" -> "sr"
            "Slovenian" -> "sl"
            "Estonian" -> "et"
            "Latvian" -> "lv"
            "Lithuanian" -> "lt"
            "Filipino" -> "fil"
            "Urdu" -> "ur"
            "Bengali" -> "bn"
            "Gujarati" -> "gu"
            "Kannada" -> "kn"
            "Malayalam" -> "ml"
            "Marathi" -> "mr"
            "Nepali" -> "ne"
            "Punjabi" -> "pa"
            "Sinhala" -> "si"
            "Tamil" -> "ta"
            "Telugu" -> "te"
            else -> "en"
        }
    }
}
