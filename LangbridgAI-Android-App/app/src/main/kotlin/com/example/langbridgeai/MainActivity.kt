package com.example.langbridgai

import android.content.Intent
import android.os.Bundle
import android.widget.Button // Import Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)

        // Set initial fragment
        if (savedInstanceState == null) {
            loadFragment(TextTranslateFragment())
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_text_translate -> {
                    loadFragment(TextTranslateFragment())
                    true
                }
                R.id.nav_speech_translate -> {
                    loadFragment(SpeechTranslateFragment())
                    true
                }
                R.id.nav_image_translate -> {
                    loadFragment(ImageTranslateFragment())
                    true
                }
                else -> false
            }
        }

        // For the "Logout" button (if added to your main activity's layout)
        val logoutButton: Button = findViewById(R.id.logoutButton) // Correct ID from activity_main.xml
        logoutButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java) // Assuming LoginActivity is your login screen
            startActivity(intent)
            finish() // Close MainActivity
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
