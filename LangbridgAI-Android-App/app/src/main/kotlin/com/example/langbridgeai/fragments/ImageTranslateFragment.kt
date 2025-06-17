package com.example.langbridgai.fragments

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider // For ViewModel
import androidx.lifecycle.lifecycleScope
import com.example.langbridgai.R
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.example.langbridgai.network.RetrofitClient
import com.example.langbridgai.network.TextTranslateRequest
import com.example.langbridgeai.SharedViewModel // Import SharedViewModel
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.ByteArrayOutputStream

class ImageTranslateFragment : Fragment() {

    private lateinit var uploadImageButton: Button
    private lateinit var liveCameraButton: Button
    // Removed fromLanguageSpinner and toLanguageSpinner as they are now global in MainActivity
    private lateinit var imagePreview: ImageView
    private lateinit var extractedTextView: TextView
    private lateinit var imageTranslationResult: TextView
    private lateinit var downloadButton: Button

    private val PICK_IMAGE_REQUEST = 100
    private val REQUEST_IMAGE_CAPTURE = 101
    private val CAMERA_PERMISSION_CODE = 2

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private lateinit var sharedViewModel: SharedViewModel // Declare SharedViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_image_translate, container, false)

        // Initialize SharedViewModel
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        uploadImageButton = view.findViewById(R.id.button_upload_image)
        liveCameraButton = view.findViewById(R.id.button_live_camera)
        // REMOVED: fromLanguageSpinner = view.findViewById(R.id.spinner_image_from_language)
        // REMOVED: toLanguageSpinner = view.findViewById(R.id.spinner_image_to_language)
        imagePreview = view.findViewById(R.id.image_preview)
        extractedTextView = view.findViewById(R.id.text_view_extracted_text)
        imageTranslationResult = view.findViewById(R.id.text_view_image_translation_result)
        downloadButton = view.findViewById(R.id.button_download_image_translation)

        uploadImageButton.setOnClickListener {
            openImageChooser()
        }

        liveCameraButton.setOnClickListener {
            checkCameraPermissionAndOpenCamera()
        }

        downloadButton.setOnClickListener {
            downloadImageTranslation()
        }

        // Auto-translate as user types (for prototyping the text flow)
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
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun checkCameraPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        } else {
            openCamera()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(requireContext(), "Camera permission denied.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (cameraIntent.resolveActivity(requireActivity().packageManager) != null) {
            startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE)
        } else {
            Toast.makeText(requireContext(), "No camera app found.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                PICK_IMAGE_REQUEST -> {
                    val imageUri: Uri? = data?.data
                    imageUri?.let {
                        imagePreview.setImageURI(it)
                        processImageForTextRecognition(it)
                    }
                }
                REQUEST_IMAGE_CAPTURE -> {
                    val imageBitmap = data?.extras?.get("data") as? Bitmap
                    imageBitmap?.let {
                        imagePreview.setImageBitmap(it)
                        val image = InputImage.fromBitmap(it, 0)
                        recognizeTextFromImage(image)
                    }
                }
            }
        }
    }

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
        extractedTextView.text = "Extracting text..."
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
                    imageTranslationResult.text = "No text found in image."
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Text recognition failed: ${e.message}", Toast.LENGTH_SHORT).show()
                extractedTextView.text = "Error extracting text."
                e.printStackTrace()
            }
    }

    private fun performTextTranslationFromImage(extractedText: String, sourceLang: String?, targetLang: String) {
        if (extractedText.isEmpty()) {
            imageTranslationResult.text = ""
            return
        }
        imageTranslationResult.text = "Translating image text..."

        lifecycleScope.launch {
            try {
                val requestSourceLanguage: String? = if (sourceLang == "auto") null else sourceLang
                val request = TextTranslateRequest(extractedText, requestSourceLanguage, targetLang)
                val response = RetrofitClient.apiService.translateText(request)

                if (response.isSuccessful && response.body() != null) {
                    imageTranslationResult.text = response.body()!!.translatedText
                } else {
                    val errorBody = response.errorBody()?.string()
                    imageTranslationResult.text = "Error: ${response.code()} - ${errorBody ?: response.message()}"
                    println("API Error: $errorBody")
                }
            } catch (e: Exception) {
                imageTranslationResult.text = "Network error: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    private fun uploadImageFile(imageFile: File, sourceLang: String, targetLang: String) {
        if (!imageFile.exists()) {
            Toast.makeText(requireContext(), "Image file not found!", Toast.LENGTH_SHORT).show()
            return
        }

        val requestBody = imageFile.asRequestBody("image/png".toMediaTypeOrNull())
        val imagePart = MultipartBody.Part.createFormData("image_file", imageFile.name, requestBody)

        val sourceLangBody = sourceLang.toRequestBody("text/plain".toMediaTypeOrNull())
        val targetLangBody = targetLang.toRequestBody("text/plain".toMediaTypeOrNull())

        lifecycleScope.launch {
            try {
                // val response = RetrofitClient.apiService.translateImage(imagePart, sourceLangBody, targetLangBody)
                // Handle response...
            } catch (e: Exception) {
                // Handle error...
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

    private fun downloadImageTranslation() {
        val extracted = extractedTextView.text.toString()
        val translation = imageTranslationResult.text.toString()
        if (extracted.isEmpty() || translation.isEmpty() || translation.contains("Translating...") || translation.contains("Error:")) {
            Toast.makeText(requireContext(), "No valid image translation to download", Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(requireContext(), "Downloading image translation (simulated)...", Toast.LENGTH_SHORT).show()
    }

    // Removed getLanguageCode helper function as it is now in MainActivity
}
