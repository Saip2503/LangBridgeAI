package com.example.langbridgai.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.langbridgai.MainActivity // Import MainActivity
import com.example.langbridgai.R
import com.example.langbridgai.SharedViewModel

class AccountFragment : Fragment() {

    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var usernameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var viewHistoryButton: Button
    private lateinit var logoutButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_account, container, false)

        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        usernameTextView = view.findViewById(R.id.text_view_username)
        emailTextView = view.findViewById(R.id.text_view_email)
        viewHistoryButton = view.findViewById(R.id.button_view_history)
        logoutButton = view.findViewById(R.id.button_logout)

        // Observe username from SharedViewModel and update UI
        sharedViewModel.userName.observe(viewLifecycleOwner) { username ->
            "Username: ${username ?: "Guest"}".also { usernameTextView.text = it }
        }
        // Observe email from SharedViewModel and update UI
        sharedViewModel.userEmail.observe(viewLifecycleOwner) { email ->
            "Email: ${email ?: "N/A"}".also { emailTextView.text = it }
        }


        viewHistoryButton.setOnClickListener {
            // Navigate to TranslationHistoryFragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, TranslationHistoryFragment())
                .addToBackStack(null) // Allows going back to AccountFragment
                .commit()
        }

        logoutButton.setOnClickListener {
            // Call the signOutAndNavigateToLogin function from MainActivity
            // This handles both Google Sign-Out and navigating to LoginActivity
            (activity as? MainActivity)?.signOutAndNavigateToLogin()
        }

        return view
    }
}
