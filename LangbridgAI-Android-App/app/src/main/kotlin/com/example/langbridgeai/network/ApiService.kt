package com.example.langbridgai.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @GET("/")
    suspend fun getHealthCheck(): Response<Map<String, String>>

    @POST("/translate/text")
    suspend fun translateText(@Body request: TextTranslateRequest): Response<TextTranslateResponse>

    // NOTE: For speech and image, the backend expects file uploads.
    // For this prototype, if you're not implementing actual audio recording or image capture
    // and sending them as files, you might adapt to send the SIMULATED text input
    // from the fragment to the translateText endpoint instead, for simplicity.
    // Below is how you would define it for actual file upload to your backend.

    @Multipart
    @POST("/translate/speech")
    suspend fun translateSpeech(
        @Part audio_file: MultipartBody.Part,
        @Part("from_lang") fromLang: RequestBody,
        @Part("to_lang") toLang: RequestBody
    ): Response<SpeechTranslateResponse>

    @Multipart
    @POST("/translate/image")
    suspend fun translateImage(
        @Part image_file: MultipartBody.Part,
        @Part("from_lang") fromLang: RequestBody,
        @Part("to_lang") toLang: RequestBody
    ): Response<ImageTranslateResponse>
}
