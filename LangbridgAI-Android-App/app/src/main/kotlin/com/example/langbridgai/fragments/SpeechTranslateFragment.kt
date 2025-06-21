package com.example.langbridgai.fragments

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import java.io.File
import java.util.Locale

class SpeechTranslateFragment : Fragment() {

    private lateinit var micButton: ImageButton
    private lateinit var transcriptionResult: TextView
    private lateinit var speechTranslationResult: TextView
    private lateinit var copyButton: Button
    private var speechRecognizer: SpeechRecognizer? = null

    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var dbHelper: TranslationDbHelper

    // ActivityResultLauncher for permissions
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize ActivityResultLauncher for permissions
        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startListening()
            } else {
                Toast.makeText(requireContext(), "Record audio permission denied.", Toast.LENGTH_SHORT).show()
                micButton.isEnabled = true
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_speech_translate, container, false)

        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        dbHelper = TranslationDbHelper(requireContext())

        micButton = view.findViewById(R.id.mic_button)
        transcriptionResult = view.findViewById(R.id.text_view_transcription_result)
        speechTranslationResult = view.findViewById(R.id.text_view_speech_translation_result)
        copyButton = view.findViewById(R.id.button_copy_translation)// This line initializes downloadButton

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                transcriptionResult.text = "Listening..."
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                    else -> "Unknown error"
                }
                transcriptionResult.text = "Error: $errorMessage"
                Toast.makeText(requireContext(), "Speech recognition error: $errorMessage", Toast.LENGTH_SHORT).show()
                micButton.isEnabled = true
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val transcribedText = matches[0]
                    transcriptionResult.text = transcribedText
                    val fromLangCode = sharedViewModel.fromLanguageCode.value
                    val toLangCode = sharedViewModel.toLanguageCode.value

                    if (fromLangCode != null && toLangCode != null) {
                        performTextTranslationFromSpeech(transcribedText, fromLangCode, toLangCode)
                    } else {
                        Toast.makeText(requireContext(), "Language selection is incomplete.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    "No speech detected.".also { transcriptionResult.text = it }
                }
                micButton.isEnabled = true
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        micButton.setOnClickListener {
            micButton.isEnabled = false
            checkAudioPermissionAndStartListening()
        }

        copyButton.setOnClickListener { // Changed listener to copyButton
            copyTranslationToClipboard()
        }

        transcriptionResult.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val text = s.toString().trim()
                if (text.isNotEmpty()) {
                    val fromLangCode = sharedViewModel.fromLanguageCode.value
                    val toLangCode = sharedViewModel.toLanguageCode.value
                    if (fromLangCode != null && toLangCode != null) {
                        performTextTranslationFromSpeech(text, fromLangCode, toLangCode)
                    } else {
                        Toast.makeText(requireContext(), "Language selection is incomplete.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    speechTranslationResult.text = ""
                }
            }
        })

        return view
    }

    private fun checkAudioPermissionAndStartListening() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            startListening()
        }
    }

    private fun startListening() {
        val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            val fromLangForRecognition = if (sharedViewModel.fromLanguageCode.value == "auto") {
                Locale.getDefault().language
            } else {
                sharedViewModel.fromLanguageCode.value
            }
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, fromLangForRecognition)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer?.startListening(speechIntent)
    }

    private fun performTextTranslationFromSpeech(transcribedText: String, sourceLang: String?, targetLang: String) {
        if (transcribedText.isEmpty()) {
            speechTranslationResult.text = ""
            return
        }

        if (sourceLang != "auto" && sourceLang == targetLang) {
            "Source and target languages cannot be the same for translation.".also { speechTranslationResult.text = it }
            Toast.makeText(requireContext(), "Cannot translate to the same language.", Toast.LENGTH_SHORT).show()
            return
        }

        "Translating speech...".also { speechTranslationResult.text = it }

        lifecycleScope.launch {
            try {
                val requestSourceLanguage: String? = if (sourceLang == "auto") null else sourceLang
                val request = TextTranslateRequest(transcribedText, requestSourceLanguage, targetLang)
                val response = RetrofitClient.apiService.translateText(request)

                if (response.isSuccessful && response.body() != null) {
                    val translatedText = response.body()!!.translatedText
                    speechTranslationResult.text = translatedText

                    withContext(Dispatchers.IO) {
                        val newRowId = dbHelper.insertTranslation(
                            transcribedText,
                            translatedText,
                            requestSourceLanguage,
                            targetLang
                        )
                        if (newRowId != -1L) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(requireContext(), "Speech translation saved to history!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(requireContext(), "Failed to save speech translation history.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                } else {
                    val errorBody = response.errorBody()?.string()
                    "Error: ${response.code()} - ${errorBody ?: response.message()}".also { speechTranslationResult.text = it }
                    println("API Error: $errorBody")
                }
            } catch (e: Exception) {
                "Network error: ${e.message}".also { speechTranslationResult.text = it }
                e.printStackTrace()
            }
        }
    }

    private fun uploadAudioFile(audioFilePath: String, sourceLang: String, targetLang: String) {
        val audioFile = File(audioFilePath)
        if (!audioFile.exists()) {
            Toast.makeText(requireContext(), "Audio file not found!", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                // val requestBody = audioFile.asRequestBody("audio/wav".toMediaTypeOrNull())
                // val audioPart = MultipartBody.Part.createFormData("audio_file", audioFile.name, requestBody)
                // val sourceLangBody = sourceLang.toRequestBody("text/plain".toMediaTypeOrNull())
                // val targetLangBody = targetLang.toRequestBody("text/plain".toMediaTypeOrNull())
                // val response = RetrofitClient.apiService.translateSpeech(audioPart, sourceLangBody, targetLangBody)
                // Handle response...
            } catch (e: Exception) {
                // Handle error...
            }
        }
    }


    private fun copyTranslationToClipboard() { // New function to copy to clipboard
        val textToCopy = speechTranslationResult.text.toString()
        if (textToCopy.isEmpty() || textToCopy.contains("Translating...") || textToCopy.contains("Error:")) {
            Toast.makeText(requireContext(), "No valid translation to copy", Toast.LENGTH_SHORT).show()
            return
        }

        val clipboardManager = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("Speech Translated Text", textToCopy)
        clipboardManager.setPrimaryClip(clipData)
        Toast.makeText(requireContext(), "Translation copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        speechRecognizer?.destroy()
    }
}
