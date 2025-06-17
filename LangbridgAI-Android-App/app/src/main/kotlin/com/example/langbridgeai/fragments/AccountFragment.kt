package com.example.langbridgai.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.langbridgai.LoginActivity
import com.example.langbridgai.R
import com.example.langbridgeai.SharedViewModel

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
            usernameTextView.text = "Username: ${username ?: "Guest"}"
            emailTextView.text = "Email: ${username ?: "N/A"}" // For now, email is same as username
        }

        viewHistoryButton.setOnClickListener {
            // Navigate to TranslationHistoryFragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, TranslationHistoryFragment())
                .addToBackStack(null) // Allows going back to AccountFragment
                .commit()
        }

        logoutButton.setOnClickListener {
            // Perform logout action
            // In a real app, you would clear user session/data
            sharedViewModel.setUserName(null) // Clear username on logout

            // Navigate back to LoginActivity
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clear back stack
            startActivity(intent)
            requireActivity().finish() // Finish MainActivity
        }

        return view
    }
}
