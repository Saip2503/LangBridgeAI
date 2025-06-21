package com.example.langbridgai.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.langbridgai.R
import com.example.langbridgai.SharedViewModel
import com.example.langbridgai.database.TranslationDbHelper
import com.example.langbridgai.network.RetrofitClient
import com.example.langbridgai.network.TextTranslateRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TextTranslateFragment : Fragment() {

    // Removed fromLanguageSpinner and toLanguageSpinner declarations
    private lateinit var inputText: EditText
    private lateinit var translateButton: Button
    private lateinit var translationResult: TextView
    private lateinit var copyButton: Button

    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var dbHelper: TranslationDbHelper // Declare DbHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_text_translate, container, false)

        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        dbHelper = TranslationDbHelper(requireContext()) // Initialize DbHelper

        // Removed findViewById calls for fromLanguageSpinner and toLanguageSpinner
        inputText = view.findViewById(R.id.edit_text_input)
        translateButton = view.findViewById(R.id.button_translate)
        translationResult = view.findViewById(R.id.text_view_translation_result)
        copyButton = view.findViewById(R.id.button_copy_translation)

        translateButton.setOnClickListener {
            performTextTranslation()
        }

        copyButton.setOnClickListener { // Changed listener to copyButton
            copyTranslationToClipboard()
        }

        return view
    }

    private fun performTextTranslation() {
        val sourceText = inputText.text.toString().trim()
        val fromLangCode = sharedViewModel.fromLanguageCode.value // Get from global ViewModel
        val toLangCode = sharedViewModel.toLanguageCode.value // Get from global ViewModel

        if (sourceText.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter text to translate", Toast.LENGTH_SHORT).show()
            return
        }

        if (fromLangCode == null || toLangCode == null) {
            Toast.makeText(requireContext(), "Language selection is incomplete.", Toast.LENGTH_SHORT).show()
            return
        }

        // Prevent translation if source and target languages are the same AND not "auto"
        if (fromLangCode != "auto" && fromLangCode == toLangCode) {
            translationResult.text = "Source and target languages cannot be the same for translation."
            Toast.makeText(requireContext(), "Cannot translate to the same language.", Toast.LENGTH_SHORT).show()
            return
        }

        "Translating...".also { translationResult.text = it }
        translateButton.isEnabled = false

        lifecycleScope.launch {
            try {
                val requestSourceLanguage: String? = if (fromLangCode == "auto") null else fromLangCode
                val request = TextTranslateRequest(sourceText, requestSourceLanguage, toLangCode)

                val response = RetrofitClient.apiService.translateText(request)

                if (response.isSuccessful && response.body() != null) {
                    val translatedText = response.body()!!.translatedText
                    translationResult.text = translatedText
                    Toast.makeText(requireContext(), "Translation successful!", Toast.LENGTH_SHORT).show()

                    // Save translation to database on a background thread
                    withContext(Dispatchers.IO) {
                        val newRowId = dbHelper.insertTranslation(
                            sourceText,
                            translatedText,
                            requestSourceLanguage,
                            toLangCode
                        )
                        if (newRowId != -1L) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(requireContext(), "Translation saved to history!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(requireContext(), "Failed to save translation history.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = "Error: ${response.code()} - ${errorBody ?: response.message()}"
                    translationResult.text = errorMessage
                    Toast.makeText(requireContext(), "Translation failed: ${response.code()}", Toast.LENGTH_LONG).show()
                    println("API Error: $errorBody")
                }
            } catch (e: Exception) {
                translationResult.text = "Network error: ${e.message}"
                Toast.makeText(requireContext(), "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            } finally {
                translateButton.isEnabled = true
            }
        }
    }

    private fun copyTranslationToClipboard() { // New function to copy to clipboard
        val textToCopy = translationResult.text.toString()
        if (textToCopy.isEmpty() || textToCopy.contains("Translating...") || textToCopy.contains("Error:")) {
            Toast.makeText(requireContext(), "No valid translation to copy", Toast.LENGTH_SHORT).show()
            return
        }

        val clipboardManager = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("Translated Text", textToCopy)
        clipboardManager.setPrimaryClip(clipData)
        Toast.makeText(requireContext(), "Translation copied to clipboard!", Toast.LENGTH_SHORT).show()
    }
}
