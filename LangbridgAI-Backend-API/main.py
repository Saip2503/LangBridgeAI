import os
from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from google.cloud import translate_v2 as translate
from google.cloud import speech_v1p1beta1 as speech
from google.cloud import vision_v1 as vision
import base64
import io

app = FastAPI(
    title="LangbridgAI Backend",
    description="API for Text, Speech, and Image Translation using Google Cloud APIs",
    version="1.0.0",
)

# Initialize Google Cloud clients
# These clients will automatically use the credentials provided by the Cloud Run service account.
translate_client = translate.Client()
speech_client = speech.SpeechClient()
vision_client = vision.ImageAnnotatorClient()

# Get Google Cloud Project ID from environment variable (required for Translation API v2)
# Ensure this environment variable is set in Cloud Run
# For local testing, you might need to set GOOGLE_CLOUD_PROJECT = "your-gcp-project-id"
PROJECT_ID = os.environ.get("GOOGLE_CLOUD_PROJECT")
if not PROJECT_ID:
    # Fallback for local testing if you have default credentials configured,
    # but highly recommend setting it explicitly in Cloud Run.
    print("WARNING: GOOGLE_CLOUD_PROJECT environment variable not set. Some API calls might fail.")


# --- Request Models ---
class TextTranslateRequest(BaseModel):
    text: str
    from_lang: str = "en" # Default to English
    to_lang: str = "ko"   # Default to Korean

class SpeechTranslateRequest(BaseModel):
    from_lang: str = "en" # Default to English
    to_lang: str = "ko"   # Default to Korean
    # Audio content will be sent as a file upload

class ImageTranslateRequest(BaseModel):
    from_lang: str = "en" # Default to English
    to_lang: str = "ko"   # Default to Korean
    # Image content will be sent as a file upload

# --- Helper Function for Translation ---
def _translate_text(text: str, target_language: str, source_language: str = None) -> str:
    if not text:
        return ""
    
    # Cloud Translation API expects a list of texts
    response = translate_client.translate(
        text,
        target_language=target_language,
        source_language=source_language,
        model='nmt', # Use Neural Machine Translation model
        project_id=PROJECT_ID # Required for v2 client when not inferable
    )
    # The response is a list, and we're translating a single text
    return response['translatedText']


# --- Endpoints ---

@app.get("/")
async def health_check():
    return {"status": "ok", "message": "LangbridgAI Backend is running!"}

@app.post("/translate/text")
async def translate_text_endpoint(request: TextTranslateRequest):
    """
    Translates text from one language to another.
    """
    try:
        translated_text = _translate_text(
            request.text,
            request.to_lang,
            request.from_lang
        )
        return JSONResponse(content={"translated_text": translated_text})
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Text translation failed: {e}")

@app.post("/translate/speech")
async def translate_speech_endpoint(
    audio_file: UploadFile = File(...),
    from_lang: str = "en",
    to_lang: str = "ko"
):
    """
    Transcribes audio and then translates the transcribed text.
    Supports WAV, FLAC, LINEAR16, OGG_OPUS (ensure proper encoding and sample rate).
    """
    try:
        audio_content = await audio_file.read()

        # Step 1: Transcribe audio using Google Cloud Speech-to-Text
        audio = speech.RecognitionAudio(content=audio_content)
        config = speech.RecognitionConfig(
            encoding=speech.RecognitionConfig.AudioEncoding.LINEAR16, # Or FLAC, OGG_OPUS, etc.
            sample_rate_hertz=16000, # Adjust according to your audio
            language_code=from_lang,
            enable_automatic_punctuation=True,
        )

        # Handle long audio files if necessary (e.g., using long_running_recognize)
        # For Cloud Run, keeping requests short is better.
        response = speech_client.recognize(config=config, audio=audio)

        transcribed_text = ""
        for result in response.results:
            transcribed_text += result.alternatives[0].transcript + " "
        transcribed_text = transcribed_text.strip()

        if not transcribed_text:
            return JSONResponse(content={
                "transcribed_text": "",
                "translated_text": "No speech detected or transcribed."
            })

        # Step 2: Translate the transcribed text
        translated_text = _translate_text(transcribed_text, to_lang, from_lang)

        return JSONResponse(content={
            "transcribed_text": transcribed_text,
            "translated_text": translated_text
        })

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Speech translation failed: {e}. Ensure audio encoding and sample rate are correct.")

@app.post("/translate/image")
async def translate_image_endpoint(
    image_file: UploadFile = File(...),
    from_lang: str = "en",
    to_lang: str = "ko"
):
    """
    Extracts text from an image (OCR) and then translates the extracted text.
    """
    try:
        image_content = await image_file.read()

        # Step 1: Extract text (OCR) using Google Cloud Vision
        image = vision.Image(content=image_content)
        response = vision_client.text_detection(image=image)
        texts = response.text_annotations

        extracted_text = ""
        if texts:
            # The first annotation is the full text extracted
            extracted_text = texts[0].description
        else:
            return JSONResponse(content={
                "extracted_text": "",
                "translated_text": "No text found in the image."
            })

        # Step 2: Translate the extracted text
        translated_text = _translate_text(extracted_text, to_lang, from_lang)

        return JSONResponse(content={
            "extracted_text": extracted_text,
            "translated_text": translated_text
        })

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Image translation failed: {e}")
