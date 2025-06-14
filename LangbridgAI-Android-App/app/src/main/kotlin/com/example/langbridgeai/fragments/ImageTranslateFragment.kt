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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

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
        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                val extractedText = visionText.text
                extractedTextView.text = extractedText
                if (extractedText.isNotEmpty()) {
                    performImageTranslation(extractedText)
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

    private fun performImageTranslation(extractedText: String) {
        // In a real app, you would make an API call here to translate `extractedText`.
        // Example placeholder:
        val translatedImageText = "Image Translated: $extractedText (into target language)"
        imageTranslationResult.text = translatedImageText
        Toast.makeText(requireContext(), "Translating image text...", Toast.LENGTH_SHORT).show()
    }

    private fun downloadImageTranslation() {
        val translatedText = imageTranslationResult.text.toString()
        if (translatedText.isEmpty() || translatedText == "Translated text will show here...") {
            Toast.makeText(requireContext(), "No image translation to download", Toast.LENGTH_SHORT).show()
            return
        }
        // Implement file download logic here
        Toast.makeText(requireContext(), "Downloading image translation...", Toast.LENGTH_SHORT).show()
    }
}
