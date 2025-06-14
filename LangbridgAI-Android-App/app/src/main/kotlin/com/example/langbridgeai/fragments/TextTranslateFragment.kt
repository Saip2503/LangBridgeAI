package com.example.langbridgai

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope // For coroutines scope in fragments
import com.example.langbridgai.network.RetrofitClient
import com.example.langbridgai.network.TextTranslateRequest
import kotlinx.coroutines.launch

class TextTranslateFragment : Fragment() {

    private lateinit var fromLanguageSpinner: Spinner
    private lateinit var toLanguageSpinner: Spinner
    private lateinit var inputText: EditText
    private lateinit var translateButton: Button
    private lateinit var translationResult: TextView
    private lateinit var downloadButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_text_translate, container, false)

        fromLanguageSpinner = view.findViewById(R.id.spinner_from_language)
        toLanguageSpinner = view.findViewById(R.id.spinner_to_language)
        inputText = view.findViewById(R.id.edit_text_input)
        translateButton = view.findViewById(R.id.button_translate)
        translationResult = view.findViewById(R.id.text_view_translation_result)
        downloadButton = view.findViewById(R.id.button_download_translation)

        translateButton.setOnClickListener {
            performTextTranslation()
        }

        downloadButton.setOnClickListener {
            downloadTranslation()
        }

        return view
    }

    private fun performTextTranslation() {
        val sourceText = inputText.text.toString().trim()
        val fromLangCode = getLanguageCode(fromLanguageSpinner.selectedItem.toString())
        val toLangCode = getLanguageCode(toLanguageSpinner.selectedItem.toString())

        if (sourceText.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter text to translate", Toast.LENGTH_SHORT).show()
            return
        }

        translationResult.text = "Translating..."
        translateButton.isEnabled = false // Disable button during translation

        // Use lifecycleScope for coroutines in fragments, which cancels when fragment is destroyed
        lifecycleScope.launch {
            try {
                val request = TextTranslateRequest(sourceText, fromLangCode, toLangCode)
                val response = RetrofitClient.apiService.translateText(request)

                if (response.isSuccessful && response.body() != null) {
                    translationResult.text = response.body()!!.translatedText
                    Toast.makeText(requireContext(), "Translation successful!", Toast.LENGTH_SHORT).show()
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = "Error: ${response.code()} - ${errorBody ?: response.message()}"
                    translationResult.text = errorMessage
                    Toast.makeText(requireContext(), "Translation failed: ${response.code()}", Toast.LENGTH_LONG).show()
                    println("API Error: $errorBody") // Log error for debugging
                }
            } catch (e: Exception) {
                translationResult.text = "Network error: ${e.message}"
                Toast.makeText(requireContext(), "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace() // Print stack trace for debugging
            } finally {
                translateButton.isEnabled = true // Re-enable button
            }
        }
    }

    private fun downloadTranslation() {
        val translatedText = translationResult.text.toString()
        if (translatedText.isEmpty() || translatedText.contains("Translating...") || translatedText.contains("Error:")) {
            Toast.makeText(requireContext(), "No valid translation to download", Toast.LENGTH_SHORT).show()
            return
        }
        // Basic placeholder for download. In a real app, you'd save to device storage.
        Toast.makeText(requireContext(), "Downloading translation (simulated)...", Toast.LENGTH_SHORT).show()
        // Example: You would implement file writing logic here
        // val fileName = "translation_${System.currentTimeMillis()}.txt"
        // val fileContent = translatedText
        // File(context?.filesDir, fileName).writeText(fileContent)
    }

    // Helper function to map language names to codes expected by the backend
    private fun getLanguageCode(languageName: String): String {
        return when (languageName) {
            "English" -> "en"
            "Korean" -> "ko"
            "Spanish" -> "es"
            "French" -> "fr"
            // Add more mappings as needed based on your languages_array and backend support
            else -> "en" // Default to English if not found
        }
    }
}
