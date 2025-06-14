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
        val fromLang = fromLanguageSpinner.selectedItem.toString()
        val toLang = toLanguageSpinner.selectedItem.toString()

        if (sourceText.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter text to translate", Toast.LENGTH_SHORT).show()
            return
        }

        // In a real app, you would make an API call here.
        // Example using a placeholder:
        val translatedText = "Translated from $fromLang to $toLang: $sourceText"
        translationResult.text = translatedText
        Toast.makeText(requireContext(), "Translation in progress...", Toast.LENGTH_SHORT).show()
        // Here you'd call your translation API (e.g., Google Translate API, custom backend)
        // using Retrofit or similar, and update translationResult.text on success.
    }

    private fun downloadTranslation() {
        val translatedText = translationResult.text.toString()
        if (translatedText.isEmpty() || translatedText == "Translation will appear here") {
            Toast.makeText(requireContext(), "No translation to download", Toast.LENGTH_SHORT).show()
            return
        }
        // Implement file download logic here (e.g., save to a text file in Downloads folder)
        Toast.makeText(requireContext(), "Downloading translation...", Toast.LENGTH_SHORT).show()
    }
}
