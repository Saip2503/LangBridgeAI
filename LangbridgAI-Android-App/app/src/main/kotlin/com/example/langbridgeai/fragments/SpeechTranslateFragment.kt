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
import java.util.Locale

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
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val transcribedText = matches[0]
                    transcriptionResult.text = transcribedText
                    // Now, send this transcribed text for translation
                    performSpeechTranslation(transcribedText)
                } else {
                    transcriptionResult.text = "No speech detected."
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        micButton.setOnClickListener {
            checkAudioPermissionAndStartListening()
        }

        downloadButton.setOnClickListener {
            downloadSpeechTranslation()
        }

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
            }
        }
    }

    private fun startListening() {
        val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault()) // Or set a specific language
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
        }
        speechRecognizer?.startListening(speechIntent)
    }

    private fun performSpeechTranslation(transcribedText: String) {
        // In a real app, you would make an API call here to translate `transcribedText`.
        // Example placeholder:
        val translatedSpeech = "Speech Translated: $transcribedText (into target language)"
        speechTranslationResult.text = translatedSpeech
        Toast.makeText(requireContext(), "Translating speech...", Toast.LENGTH_SHORT).show()
    }

    private fun downloadSpeechTranslation() {
        val translatedText = speechTranslationResult.text.toString()
        if (translatedText.isEmpty() || translatedText == "Translation will appear here") {
            Toast.makeText(requireContext(), "No speech translation to download", Toast.LENGTH_SHORT).show()
            return
        }
        // Implement file download logic here
        Toast.makeText(requireContext(), "Downloading speech translation...", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        speechRecognizer?.destroy() // Release resources
    }
}
