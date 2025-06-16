package com.example.langbridgai.network

import com.google.gson.annotations.SerializedName

// --- Request Models (for sending data to backend) ---

data class TextTranslateRequest(
    val text: String,
    @SerializedName("source_language") val sourceLanguage: String?, // Use Optional for source_language, align with backend
    @SerializedName("target_language") val targetLanguage: String // Align field name with backend
)

// --- Response Models (for receiving data from backend) ---

data class TextTranslateResponse(
    @SerializedName("translatedText") val translatedText: String // Assuming backend returns 'translatedText' as it did before
)

// For speech translation, the backend returns transcribed_text and translated_text
data class SpeechTranslateResponse(
    @SerializedName("transcribed_text") val transcribedText: String,
    @SerializedName("translated_text") val translatedText: String
)

// For image translation, the backend returns extracted_text and translated_text
data class ImageTranslateResponse(
    @SerializedName("extracted_text") val extractedText: String,
    @SerializedName("translated_text") val translatedText: String
)

// Generic error response (if your backend returns a structured error)
data class ErrorResponse(
    val detail: String
)
