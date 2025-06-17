package com.example.langbridgai.fragments

import android.Manifest
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.langbridgai.R
import com.example.langbridgeai.SharedViewModel
import com.example.langbridgai.network.RetrofitClient
import com.example.langbridgai.network.TextTranslateRequest
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.Locale

class SpeechTranslateFragment : Fragment() {

    private lateinit var micButton: ImageButton
    private lateinit var transcriptionResult: TextView
    private lateinit var speechTranslationResult: TextView
    private lateinit var downloadButton: Button
    private var speechRecognizer: SpeechRecognizer? = null
    private val RECORD_AUDIO_PERMISSION_CODE = 1

    private lateinit var sharedViewModel: SharedViewModel // Declare SharedViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_speech_translate, container, false)

        // Initialize SharedViewModel from the activity
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        micButton = view.findViewById(R.id.mic_button)
        transcriptionResult = view.findViewById(R.id.text_view_transcription_result)
        speechTranslationResult = view.findViewById(R.id.text_view_speech_translation_result)
        downloadButton = view.findViewById(R.id.button_download_speech_translation)

        // Initialize SpeechRecognizer
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
                    // Get languages from global ViewModel
                    val fromLangCode = sharedViewModel.fromLanguageCode.value
                    val toLangCode = sharedViewModel.toLanguageCode.value

                    if (fromLangCode != null && toLangCode != null) {
                        performTextTranslationFromSpeech(transcribedText, fromLangCode, toLangCode)
                    } else {
                        Toast.makeText(requireContext(), "Language selection is incomplete.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    transcriptionResult.text = "No speech detected."
                }
                micButton.isEnabled = true
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        micButton.setOnClickListener {
            micButton.isEnabled = false // Disable mic button while listening
            checkAudioPermissionAndStartListening()
        }

        downloadButton.setOnClickListener {
            downloadSpeechTranslation()
        }

        // Auto-translate as user types (for prototyping the text flow)
        transcriptionResult.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val text = s.toString().trim()
                if (text.isNotEmpty()) {
                    // Get languages from global ViewModel
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
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_PERMISSION_CODE)
        } else {
            startListening()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startListening()
            } else {
                Toast.makeText(requireContext(), "Record audio permission denied.", Toast.LENGTH_SHORT).show()
                micButton.isEnabled = true
            }
        }
    }

    private fun startListening() {
        val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            // Use the global 'from' language for recognition if it's not 'auto'
            val fromLangForRecognition = if (sharedViewModel.fromLanguageCode.value == "auto") {
                Locale.getDefault().language // Fallback to device locale for recognition
            } else {
                sharedViewModel.fromLanguageCode.value
            }
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, fromLangForRecognition)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1) // Get only the top result
        }
        speechRecognizer?.startListening(speechIntent)
    }

    // This function sends transcribed text to the backend's TEXT translation endpoint
    private fun performTextTranslationFromSpeech(transcribedText: String, sourceLang: String?, targetLang: String) {
        if (transcribedText.isEmpty()) {
            speechTranslationResult.text = ""
            return
        }

        // Prevent translation if source and target languages are the same AND not "auto"
        if (sourceLang != "auto" && sourceLang == targetLang) {
            speechTranslationResult.text = "Source and target languages cannot be the same for translation."
            Toast.makeText(requireContext(), "Cannot translate to the same language.", Toast.LENGTH_SHORT).show()
            return
        }

        speechTranslationResult.text = "Translating speech..."

        lifecycleScope.launch {
            try {
                val requestSourceLanguage: String? = if (sourceLang == "auto") null else sourceLang
                val request = TextTranslateRequest(transcribedText, requestSourceLanguage, targetLang)
                val response = RetrofitClient.apiService.translateText(request)

                if (response.isSuccessful && response.body() != null) {
                    speechTranslationResult.text = response.body()!!.translatedText
                } else {
                    val errorBody = response.errorBody()?.string()
                    speechTranslationResult.text = "Error: ${response.code()} - ${errorBody ?: response.message()}"
                    println("API Error: $errorBody")
                }
            } catch (e: Exception) {
                speechTranslationResult.text = "Network error: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    // This is an EXAMPLE of how you would prepare for ACTUAL audio file upload
    // if you were sending to the /translate/speech endpoint that expects a file.
    // This requires actual audio recording/file handling, which is out of scope for this quick example.
    private fun uploadAudioFile(audioFilePath: String, sourceLang: String, targetLang: String) {
        val audioFile = File(audioFilePath)
        if (!audioFile.exists()) {
            Toast.makeText(requireContext(), "Audio file not found!", Toast.LENGTH_SHORT).show()
            return
        }

        val requestBody = audioFile.asRequestBody("audio/wav".toMediaTypeOrNull()) // Adjust MIME type
        val audioPart = MultipartBody.Part.createFormData("audio_file", audioFile.name, requestBody)

        val sourceLangBody = sourceLang.toRequestBody("text/plain".toMediaTypeOrNull())
        val targetLangBody = targetLang.toRequestBody("text/plain".toMediaTypeOrNull())

        lifecycleScope.launch {
            try {
                // val response = RetrofitClient.apiService.translateSpeech(audioPart, sourceLangBody, targetLangBody)
                // Handle response...
            } catch (e: Exception) {
                // Handle error...
            }
        }
    }


    private fun downloadSpeechTranslation() {
        val transcription = transcriptionResult.text.toString()
        val translatedText = speechTranslationResult.text.toString()
        if (transcription.isEmpty() || translatedText.isEmpty() || translatedText.contains("Translating...") || translatedText.contains("Error:")) {
            Toast.makeText(requireContext(), "No valid speech translation to download", Toast.LENGTH_SHORT).show()
            return
        }
        // Implement file download logic here
        Toast.makeText(requireContext(), "Downloading speech translation (simulated)...", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        speechRecognizer?.destroy() // Release resources
    }

    // Removed getLanguageCode helper function as it is now in MainActivity
}
