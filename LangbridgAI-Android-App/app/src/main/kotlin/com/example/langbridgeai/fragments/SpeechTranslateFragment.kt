package com.example.langbridgai

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
import androidx.lifecycle.lifecycleScope
import com.example.langbridgai.network.RetrofitClient
import com.example.langbridgai.network.TextTranslateRequest
import kotlinx.coroutines.launch
import java.util.Locale
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File // Used if you were to prepare an actual file for upload

class SpeechTranslateFragment : Fragment() {

    private lateinit var micButton: ImageButton
    private lateinit var transcriptionResult: TextView
    private lateinit var speechTranslationResult: TextView
    private lateinit var downloadButton: Button
    private var speechRecognizer: SpeechRecognizer? = null
    private val RECORD_AUDIO_PERMISSION_CODE = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_speech_translate, container, false)

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
                    // Now, send this transcribed text for translation
                    // NOTE: For this prototype, we send the transcribed text to the backend's text translation endpoint.
                    // A full implementation would send actual audio to the /translate/speech endpoint.
                    performTextTranslationFromSpeech(transcribedText)
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
                    // Send typed text to the backend's text translation endpoint
                    performTextTranslationFromSpeech(text)
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
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault()) // Or set a specific language
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1) // Get only the top result
        }
        speechRecognizer?.startListening(speechIntent)
    }

    // This function sends transcribed text to the backend's TEXT translation endpoint
    private fun performTextTranslationFromSpeech(transcribedText: String) {
        if (transcribedText.isEmpty()) {
            speechTranslationResult.text = ""
            return
        }

        speechTranslationResult.text = "Translating speech..."
        // In a real app, you might have specific 'from' and 'to' languages for speech
        // For this prototype, we can use default or take from another spinner if available.
        val fromLangCode = "en" // Assuming English transcription for now
        val toLangCode = "en" // You might get this from a global setting or another spinner

        lifecycleScope.launch {
            try {
                val request = TextTranslateRequest(transcribedText, fromLangCode, toLangCode)
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
    private fun uploadAudioFile(audioFilePath: String, fromLang: String, toLang: String) {
        val audioFile = File(audioFilePath)
        if (!audioFile.exists()) {
            Toast.makeText(requireContext(), "Audio file not found!", Toast.LENGTH_SHORT).show()
            return
        }

        val requestBody = audioFile.asRequestBody("audio/wav".toMediaTypeOrNull()) // Adjust MIME type
        val audioPart = MultipartBody.Part.createFormData("audio_file", audioFile.name, requestBody)

        val fromLangBody = fromLang.toRequestBody("text/plain".toMediaTypeOrNull())
        val toLangBody = toLang.toRequestBody("text/plain".toMediaTypeOrNull())

        lifecycleScope.launch {
            try {
                // val response = RetrofitClient.apiService.translateSpeech(audioPart, fromLangBody, toLangBody)
                // Handle response...
            } catch (e: Exception) {
                // Handle error...
            }
        }
    }


    private fun downloadSpeechTranslation() {
        val transcription = transcriptionResult.text.toString()
        val translatedText = speechTranslationResult.text.toString()
        if (translatedText.isEmpty() || translatedText.contains("Translating...") || translatedText.contains("Error:")) {
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
}
