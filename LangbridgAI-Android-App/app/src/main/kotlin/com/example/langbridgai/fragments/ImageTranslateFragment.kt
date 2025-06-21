package com.example.langbridgai.fragments

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class ImageTranslateFragment : Fragment() {

    private lateinit var uploadImageButton: Button
    private lateinit var liveCameraButton: Button
    private lateinit var imagePreview: ImageView
    private lateinit var extractedTextView: TextView
    private lateinit var imageTranslationResult: TextView
    private lateinit var copyButton: Button

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var dbHelper: TranslationDbHelper

    // ActivityResultLaunchers for new API
    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    private lateinit var takePictureLauncher: ActivityResultLauncher<Void?>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize ActivityResultLaunchers in onCreate
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                imagePreview.setImageURI(it)
                processImageForTextRecognition(it)
            }
        }

        takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
            bitmap?.let {
                imagePreview.setImageBitmap(it)
                val image = InputImage.fromBitmap(it, 0)
                recognizeTextFromImage(image)
            }
        }

        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                openCamera()
            } else {
                Toast.makeText(requireContext(), "Camera permission denied.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_image_translate, container, false)

        sharedViewModel = ViewModelProvider(requireActivity()).get(modelClass = SharedViewModel::class.java)
        dbHelper = TranslationDbHelper(requireContext())

        uploadImageButton = view.findViewById(R.id.button_upload_image)
        liveCameraButton = view.findViewById(R.id.button_live_camera)
        imagePreview = view.findViewById(R.id.image_preview)
        extractedTextView = view.findViewById(R.id.text_view_extracted_text)
        imageTranslationResult = view.findViewById(R.id.text_view_image_translation_result)
        copyButton = view.findViewById(R.id.button_copy_translation)

        uploadImageButton.setOnClickListener {
            openImageChooser()
        }

        liveCameraButton.setOnClickListener {
            checkCameraPermissionAndOpenCamera()
        }

        copyButton.setOnClickListener { // Changed listener to copyButton
            copyTranslationToClipboard()
        }

        extractedTextView.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val text = s.toString().trim()
                if (text.isNotEmpty()) {
                    val fromLangCode = sharedViewModel.fromLanguageCode.value
                    val toLangCode = sharedViewModel.toLanguageCode.value
                    if (fromLangCode != null && toLangCode != null) {
                        performTextTranslationFromImage(text, fromLangCode, toLangCode)
                    } else {
                        Toast.makeText(requireContext(), "Language selection is incomplete.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    imageTranslationResult.text = ""
                }
            }
        })

        return view
    }

    private fun openImageChooser() {
        pickImageLauncher.launch("image/*") // Use launcher
    }

    private fun checkCameraPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA) // Use launcher
        } else {
            openCamera()
        }
    }

    private fun openCamera() {
        takePictureLauncher.launch(null) // Use launcher, passing null as we want a thumbnail bitmap
    }

    // onActivityResult and onRequestPermissionsResult are no longer needed
    // The logic is now handled in the ActivityResultLauncher callbacks in onCreate

    private fun processImageForTextRecognition(imageUri: Uri) {
        val image: InputImage
        try {
            image = InputImage.fromFilePath(requireContext(), imageUri)
            recognizeTextFromImage(image)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun recognizeTextFromImage(image: InputImage) {
        "Extracting text...".also { extractedTextView.text = it }
        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                val extractedText = visionText.text
                extractedTextView.text = extractedText
                if (extractedText.isNotEmpty()) {
                    val fromLangCode = sharedViewModel.fromLanguageCode.value
                    val toLangCode = sharedViewModel.toLanguageCode.value
                    if (fromLangCode != null && toLangCode != null) {
                        performTextTranslationFromImage(extractedText, fromLangCode, toLangCode)
                    } else {
                        Toast.makeText(requireContext(), "Language selection is incomplete.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    "No text found in image.".also { imageTranslationResult.text = it }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Text recognition failed: ${e.message}", Toast.LENGTH_SHORT).show()
                "Error extracting text.".also { extractedTextView.text = it }
                e.printStackTrace()
            }
    }

    private fun performTextTranslationFromImage(extractedText: String, sourceLang: String?, targetLang: String) {
        if (extractedText.isEmpty()) {
            imageTranslationResult.text = ""
            return
        }

        if (sourceLang != "auto" && sourceLang == targetLang) {
            "Source and target languages cannot be the same for translation.".also { imageTranslationResult.text = it }
            Toast.makeText(requireContext(), "Cannot translate to the same language.", Toast.LENGTH_SHORT).show()
            return
        }

        "Translating image text...".also { imageTranslationResult.text = it }

        lifecycleScope.launch {
            try {
                val requestSourceLanguage: String? = if (sourceLang == "auto") null else sourceLang
                val request = TextTranslateRequest(extractedText, requestSourceLanguage, targetLang)
                val response = RetrofitClient.apiService.translateText(request)

                if (response.isSuccessful && response.body() != null) {
                    val translatedText = response.body()!!.translatedText
                    imageTranslationResult.text = translatedText

                    withContext(Dispatchers.IO) {
                        val newRowId = dbHelper.insertTranslation(
                            extractedText,
                            translatedText,
                            requestSourceLanguage,
                            targetLang
                        )
                        if (newRowId != -1L) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(requireContext(), "Image translation saved to history!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(requireContext(), "Failed to save image translation history.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                } else {
                    val errorBody = response.errorBody()?.string()
                    "Error: ${response.code()} - ${errorBody ?: response.message()}".also { imageTranslationResult.text = it }
                    println("API Error: $errorBody")
                }
            } catch (e: Exception) {
                "Network error: ${e.message}".also { imageTranslationResult.text = it }
                e.printStackTrace()
            }
        }
    }

    // Placeholder for future file upload logic - removed unused variables
    private fun uploadImageFile(imageFile: File, sourceLang: String, targetLang: String) {
        if (!imageFile.exists()) {
            Toast.makeText(requireContext(), "Image file not found!", Toast.LENGTH_SHORT).show()
            return
        }

        val requestBody = imageFile.asRequestBody("image/png".toMediaTypeOrNull())
        val imagePart = MultipartBody.Part.createFormData("image_file", imageFile.name, requestBody) // This variable is still needed for actual upload

        val sourceLangBody = sourceLang.toRequestBody("text/plain".toMediaTypeOrNull()) // This variable is still needed
        val targetLangBody = targetLang.toRequestBody("text/plain".toMediaTypeOrNull()) // This variable is still needed

        lifecycleScope.launch {
            try {
                //val response = RetrofitClient.apiService.translateImage(imagePart, sourceLangBody, targetLangBody)
                // Handle response...
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Network error during image upload: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e("ImageUpload", "General Exception: ${e.message}", e)
                }
            }
        }
    }

    private fun bitmapToFile(bitmap: Bitmap, fileName: String): File? {
        val file = File(requireContext().cacheDir, fileName)
        return try {
            file.createNewFile()
            val bos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos)
            val bitmapdata = bos.toByteArray()

            val fos = FileOutputStream(file)
            fos.write(bitmapdata)
            fos.flush()
            fos.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun copyTranslationToClipboard() { // New function to copy to clipboard
        val textToCopy = imageTranslationResult.text.toString()
        if (textToCopy.isEmpty() || textToCopy.contains("Translating...") || textToCopy.contains("Error:")) {
            Toast.makeText(requireContext(), "No valid translation to copy", Toast.LENGTH_SHORT).show()
            return
        }

        val clipboardManager = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("Image Translated Text", textToCopy)
        clipboardManager.setPrimaryClip(clipData)
        Toast.makeText(requireContext(), "Translation copied to clipboard!", Toast.LENGTH_SHORT).show()
    }
}
