# LangbridgAI Backend

This repository contains the backend service for the LangbridgAI mobile application, built with FastAPI and deployed on Google Cloud Run. It leverages Google Cloud's AI services for text, speech, and image translation capabilities.

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Google Cloud APIs Used](#google-cloud-apis-used)
- [Local Development Setup](#local-development-setup)
- [Deployment to Google Cloud Run](#deployment-to-google-cloud-run)
- [API Endpoints](#api-endpoints)
- [Contributing](#contributing)
- [License](#license)

## Features

- **Text Translation**: Translates text between specified languages.
- **Speech Translation**: Transcribes spoken audio into text and then translates the transcribed text.
- **Image Translation (OCR)**: Extracts text from an image (using OCR) and then translates the extracted text.

## Architecture

The backend is a lightweight FastAPI application designed for serverless deployment on Google Cloud Run. It communicates with the following Google Cloud services:

- **Cloud Translation API**: For language translation.
- **Cloud Speech-to-Text API**: For converting spoken audio into text.
- **Cloud Vision API**: For Optical Character Recognition (OCR) to extract text from images.

## Google Cloud APIs Used

Ensure these APIs are enabled in your Google Cloud Project:

1.  **Cloud Translation API**
2.  **Cloud Speech-to-Text API**
3.  **Cloud Vision API**

You can enable them via the [Google Cloud Console](https://console.cloud.google.com/apis/library).

## Local Development Setup

To run this backend locally:

1.  **Clone the repository:**
    ```bash
    git clone [https://github.com/your-username/LangbridgAI-Backend.git](https://github.com/your-username/LangbridgAI-Backend.git)
    cd LangbridgAI-Backend
    ```

2.  **Create and activate a Python virtual environment:**
    ```bash
    python3 -m venv venv
    source venv/bin/activate  # On Windows: .\venv\Scripts\activate
    ```

3.  **Install dependencies:**
    ```bash
    pip install -r requirements.txt
    ```

4.  **Set up Google Cloud Authentication (for local testing):**
    You need to authenticate your `gcloud` CLI and set your project.
    ```bash
    gcloud auth application-default login
    gcloud config set project YOUR_PROJECT_ID
    ```
    (Replace `YOUR_PROJECT_ID` with your actual GCP Project ID).

5.  **Run the FastAPI application:**
    ```bash
    uvicorn main:app --host 0.0.0.0 --port 8000 --reload
    ```
    The API will be available at `http://localhost:8000`. The `--reload` flag enables auto-reloading on code changes.

## Deployment to Google Cloud Run

Follow these steps to deploy your backend to Cloud Run:

1.  **Ensure Google Cloud CLI and Docker are installed:**
    * [Install `gcloud` CLI](https://cloud.google.com/sdk/docs/install)
    * [Install Docker](https://docs.docker.com/get-docker/)

2.  **Authenticate Docker with Google Cloud:**
    ```bash
    gcloud auth configure-docker
    ```

3.  **Build and Push the Docker Image:**
    Replace `YOUR_PROJECT_ID` and `YOUR_SERVICE_NAME` with your actual values.
    ```bash
    export GOOGLE_CLOUD_PROJECT="YOUR_PROJECT_ID"
    export IMAGE_NAME="gcr.io/${GOOGLE_CLOUD_PROJECT}/YOUR_SERVICE_NAME:latest"

    docker build -t ${IMAGE_NAME} .
    docker push ${IMAGE_NAME}
    ```

4.  **Deploy to Google Cloud Run:**
    ```bash
    gcloud run deploy YOUR_SERVICE_NAME \
        --image ${IMAGE_NAME} \
        --platform managed \
        --region us-central1 \
        --allow-unauthenticated \
        --set-env-vars GOOGLE_CLOUD_PROJECT=${GOOGLE_CLOUD_PROJECT} \
        --cpu 1 \
        --memory 512Mi \
        --max-instances 1 \
        --min-instances 0
    ```
    (Adjust `--region` and resource settings as needed.)

5.  **Grant Service Account Permissions:**
    The Cloud Run service account needs permissions to access Google Cloud APIs.
    - Go to **IAM & Admin > IAM** in the Google Cloud Console.
    - Find the service account associated with your Cloud Run service (e.g., `YOUR_SERVICE_NAME@YOUR_PROJECT_ID.iam.gserviceaccount.com`).
    - Add the following roles:
        - `Cloud Translation API User`
        - `Cloud Speech-to-Text API User`
        - `Cloud Vision API User`

6.  **Update Frontend:**
    After deployment, `gcloud run deploy` will provide a service URL. Update your Android frontend's API base URL to point to this new Cloud Run service URL.

## API Endpoints

The API base URL will be your Cloud Run service URL (e.g., `https://langbridgai-backend-xxxxx-uc.a.run.app`).

### 1. Health Check
- **Endpoint:** `GET /`
- **Description:** Checks if the backend is running.
- **Response:** `{"status": "ok", "message": "LangbridgAI Backend is running!"}`

### 2. Text Translation
- **Endpoint:** `POST /translate/text`
- **Description:** Translates a given text string.
- **Request Body (JSON):**
    ```json
    {
        "text": "Hello, how are you?",
        "from_lang": "en",
        "to_lang": "ko"
    }
    ```
- **Response (JSON):**
    ```json
    {
        "translated_text": "안녕하세요, 잘 지내세요?"
    }
    ```

### 3. Speech Translation
- **Endpoint:** `POST /translate/speech`
- **Description:** Transcribes an audio file and then translates the transcribed text.
- **Request (multipart/form-data):**
    - `audio_file`: The audio file (e.g., `.wav`, `.flac`).
    - `from_lang` (form field): Source language code (e.g., "en").
    - `to_lang` (form field): Target language code (e.g., "ko").
- **Response (JSON):**
    ```json
    {
        "transcribed_text": "Hello, how are you?",
        "translated_text": "안녕하세요, 잘 지내세요?"
    }
    ```
    *Note: Ensure audio encoding and sample rate in `main.py` (`config`) match your audio file for best results.*

### 4. Image Translation
- **Endpoint:** `POST /translate/image`
- **Description:** Extracts text from an image (OCR) and then translates the extracted text.
- **Request (multipart/form-data):**
    - `image_file`: The image file (e.g., `.jpg`, `.png`).
    - `from_lang` (form field): Source language code (e.g., "en").
    - `to_lang` (form field): Target language code (e.g., "ko").
- **Response (JSON):**
    ```json
    {
        "extracted_text": "Hello, how are you?",
        "translated_text": "안녕하세요, 잘 지내세요?"
    }
    ```

## Contributing

Feel free to open issues or submit pull requests.

## License

This project is open source and available under the [MIT License](LICENSE).
