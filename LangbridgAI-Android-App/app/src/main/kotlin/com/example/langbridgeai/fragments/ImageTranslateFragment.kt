package com.example.langbridgai

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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.example.langbridgai.network.RetrofitClient
import com.example.langbridgai.network.TextTranslateRequest
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File // Used if you were to prepare an actual file for upload
import java.io.FileOutputStream
import java.io.ByteArrayOutputStream

class ImageTranslateFragment : Fragment() {

    private lateinit var uploadImageButton: Button
    private lateinit var liveCameraButton: Button
    private lateinit var imagePreview: ImageView
    private lateinit var extractedTextView: TextView
    private lateinit var imageTranslationResult: TextView
    private lateinit var downloadButton: Button

    private val PICK_IMAGE_REQUEST = 100
    private val REQUEST_IMAGE_CAPTURE = 101
    private val CAMERA_PERMISSION_CODE = 2

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_image_translate, container, false)

        uploadImageButton = view.findViewById(R.id.button_upload_image)
        liveCameraButton = view.findViewById(R.id.button_live_camera)
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
                    // Send typed text to the backend's text translation endpoint
                    performTextTranslationFromImage(text)
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
                        // For ML Kit, you can use Bitmap directly:
                        val image = InputImage.fromBitmap(it, 0)
                        recognizeTextFromImage(image)

                        // If you were to upload the bitmap as a file to backend:
                        // val file = bitmapToFile(it, "temp_image.png")
                        // if (file != null) {
                        //     uploadImageFile(file, "en", "en") // Example langs
                        // }
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
                    // NOTE: For this prototype, we send the extracted text to the backend's text translation endpoint.
                    // A full implementation would send actual image to the /translate/image endpoint.
                    performTextTranslationFromImage(extractedText)
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

    // This function sends extracted text to the backend's TEXT translation endpoint
    private fun performTextTranslationFromImage(extractedText: String) {
        if (extractedText.isEmpty()) {
            imageTranslationResult.text = ""
            return
        }
        imageTranslationResult.text = "Translating image text..."
        // Assume default from English to English for now or get from a spinner
        val fromLangCode = "en"
        val toLangCode = "en"

        lifecycleScope.launch {
            try {
                val request = TextTranslateRequest(extractedText, fromLangCode, toLangCode)
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

    // This is an EXAMPLE of how you would prepare for ACTUAL image file upload
    // if you were sending to the /translate/image endpoint that expects a file.
    private fun uploadImageFile(imageFile: File, fromLang: String, toLang: String) {
        if (!imageFile.exists()) {
            Toast.makeText(requireContext(), "Image file not found!", Toast.LENGTH_SHORT).show()
            return
        }

        val requestBody = imageFile.asRequestBody("image/png".toMediaTypeOrNull()) // Adjust MIME type
        val imagePart = MultipartBody.Part.createFormData("image_file", imageFile.name, requestBody)

        val fromLangBody = fromLang.toRequestBody("text/plain".toMediaTypeOrNull())
        val toLangBody = toLang.toRequestBody("text/plain".toMediaTypeOrNull())

        lifecycleScope.launch {
            try {
                // val response = RetrofitClient.apiService.translateImage(imagePart, fromLangBody, toLangBody)
                // Handle response...
            } catch (e: Exception) {
                // Handle error...
            }
        }
    }

    // Helper to convert Bitmap to File (requires WRITE_EXTERNAL_STORAGE or Scoped Storage)
    private fun bitmapToFile(bitmap: Bitmap, fileName: String): File? {
        // Create a file in cache directory
        val file = File(requireContext().cacheDir, fileName)
        return try {
            file.createNewFile()
            val bos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos) // or JPEG
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
        if (translation.isEmpty() || translation.contains("Translating...") || translation.contains("Error:")) {
            Toast.makeText(requireContext(), "No valid image translation to download", Toast.LENGTH_SHORT).show()
            return
        }
        // Implement file download logic here
        Toast.makeText(requireContext(), "Downloading image translation (simulated)...", Toast.LENGTH_SHORT).show()
    }
}
