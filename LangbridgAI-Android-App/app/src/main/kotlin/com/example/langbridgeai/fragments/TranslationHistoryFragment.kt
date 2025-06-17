package com.example.langbridgai.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.langbridgai.R
import com.example.langbridgai.database.TranslationDbHelper
import com.example.langbridgai.database.TranslationHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class TranslationHistoryFragment : Fragment() {

    private lateinit var dbHelper: TranslationDbHelper
    private lateinit var historyDisplayTextView: TextView // For simple display

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_translation_history, container, false)

        dbHelper = TranslationDbHelper(requireContext())
        historyDisplayTextView = view.findViewById(R.id.text_view_history_display)

        loadTranslationHistory()

        return view
    }

    private fun loadTranslationHistory() {
        lifecycleScope.launch {
            val historyList = withContext(Dispatchers.IO) {
                dbHelper.getAllTranslations()
            }

            if (historyList.isEmpty()) {
                historyDisplayTextView.text = "No translation history yet."
            } else {
                val formattedHistory = formatHistory(historyList)
                historyDisplayTextView.text = formattedHistory
            }
        }
    }

    private fun formatHistory(historyList: List<TranslationHistory>): String {
        val stringBuilder = StringBuilder()
        historyList.forEachIndexed { index, entry ->
            stringBuilder.append("--- Entry ${index + 1} ---\n")
            stringBuilder.append("Timestamp: ${entry.timestamp}\n")
            stringBuilder.append("From: ${entry.fromLang.uppercase(Locale.ROOT)} To: ${entry.toLang.uppercase(Locale.ROOT)}\n")
            stringBuilder.append("Original: ${entry.originalText}\n")
            stringBuilder.append("Translated: ${entry.translatedText}\n")
            stringBuilder.append("\n")
        }
        return stringBuilder.toString()
    }
}
