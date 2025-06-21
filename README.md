# LangbridgAI: AI-Powered Language Translation App



## 🚀 Project Overview



LangbridgAI is a mobile application designed to break down language barriers through advanced AI-powered translation. It offers robust capabilities across text, speech-to-text, and image (OCR) translation. The app features Google Sign-In for user authentication, stores translation history locally using SQLite, and connects a Kotlin Android frontend to a Python FastAPI backend deployed on Google Cloud Run, leveraging Google Cloud's Translation, Speech-to-Text, and Vision AI services.



## ✨ Features



* **Text Translation:** Translate typed or pasted text between a wide range of languages.

* **Speech Translation:** Convert spoken words to text (transcription) and then translate the transcribed text into a target language.

* **Image Translation:** Extract text from images using Optical Character Recognition (OCR) and translate the extracted content.

* **Global Language Selection:** Convenient "From" and "To" language spinners in the main interface to select translation languages, including "Auto-Detect" for source language.

* **Translation History:** Locally store and view all past translations using an SQLite database.

* **User Authentication:** Secure user login via a dummy credential system or seamless Google Sign-In.

* **User Profile:** An Account section to view user details and manage logout.

* **Copy Translation:** Easily copy translated text to the clipboard from any translation modality.



## 🛠️ Technologies Used



### Frontend (Android)



* **Language:** Kotlin

* **Framework:** Android (AndroidX Libraries)

* **UI Components:** Material Design, ConstraintLayout, CardView

* **Architecture:** MVVM (Model-View-ViewModel) with `ViewModel` and `LiveData`

* **Asynchronous Operations:** Kotlin Coroutines (`lifecycleScope`, `Dispatchers`)

* **API Interaction:** Retrofit2, OkHttp (with Logging Interceptor), Gson Converter

* **Authentication:** Google Sign-In SDK (`com.google.android.gms:play-services-auth`)

* **Text Recognition:** Google ML Kit Text Recognition (Latin script)

* **Local Storage:** SQLite (via `SQLiteOpenHelper`)
  ### Backend (FastAPI)

* **Language:** Python
* **Web Framework:** FastAPI
* **Deployment:** Google Cloud Run
* **Cloud Services:**
    * Google Cloud Translation API
    * Google Cloud Speech-to-Text API
    * Google Cloud Vision AI (for OCR)

## 🏗️ Architecture Overview

The LangbridgAI application follows a client-server architecture:

* **Android Frontend:** The mobile application, built with Kotlin, provides the user interface and handles user interactions. It uses Retrofit to make HTTP requests to the backend for all translation operations. User authentication is managed either locally (dummy) or via Google Sign-In, and translation history is persisted locally using SQLite.
* **Python FastAPI Backend:** This microservice is responsible for handling translation requests. It receives requests from the Android app, interacts with various Google Cloud AI services (Translation, Speech-to-Text, Vision AI), processes the data, and returns the translated results to the frontend. It is designed to be deployed serverlessly on Google Cloud Run.

## ⚙️ Setup Instructions

To get the LangbridgAI project up and running, you will need to set up both the Android frontend and the Python FastAPI backend.

### Prerequisites

* **Android Development:**
    * Android Studio (Latest stable version recommended)
    * Android SDK Platform 35 (or higher, as specified in `app/build.gradle`)
    * A physical Android device or an Android Emulator
* **Python Development:**
    * Python 3.8+
    * `pip` (Python package installer)
    * `venv` (Python virtual environment module)
* **Google Cloud Platform (GCP):**
    * A GCP Project with billing enabled.
    * **Enabled APIs:**
        * Google Cloud Translation API
        * Google Cloud Speech-to-Text API
        * Google Cloud Vision AI
        * Google People API (for Google Sign-In)
    * Service Account Key (JSON file) for backend authentication.
    * OAuth 2.0 Client IDs (Web application and Android application) for Google Sign-In.

### 1. Backend Setup (Python FastAPI)

**(Assuming your backend code is in a separate directory, e.g., `LangbridgAI-Backend/`)**

1.  **Clone the repository:**
    ```bash
    git clone [https://github.com/Saip2503/LangBridgeAI.git](https://github.com/Saip2503/LangBridgeAI.git)
    cd LangBridgeAI
    # Assuming your backend is in a subfolder, e.g., 'backend/'
    cd backend/
    ```
2.  **Create a Python virtual environment:**
    ```bash
    python -m venv venv
    ```
3.  **Activate the virtual environment:**
    * On Windows: `.\venv\Scripts\activate`
    * On macOS/Linux: `source venv/bin/activate`
4.  **Install dependencies:**
    ```bash
    pip install -r requirements.txt
    ```
    (Ensure you have a `requirements.txt` file in your backend directory listing all Python dependencies like `fastapi`, `uvicorn`, `google-cloud-translate`, `google-cloud-speech`, `google-cloud-vision`, `python-dotenv` etc.)
5.  **Google Cloud Authentication:**
    * Download your GCP Service Account Key (JSON file) and place it in your backend directory (e.g., `path/to/your-service-account-key.json`).
    * Set the `GOOGLE_APPLICATION_CREDENTIALS` environment variable to point to this file:
        * On Windows (for current session):
            ```cmd
            set GOOGLE_APPLICATION_CREDENTIALS=path\to\your-service-account-key.json
            ```
        * On macOS/Linux (for current session):
            ```bash
            export GOOGLE_APPLICATION_CREDENTIALS=/path/to/your-service-account-key.json
            ```
    * Alternatively, use a `.env` file and `python-dotenv` to load credentials, ensuring it's not committed to Git.
6.  **Run the Backend Locally (for testing):**
    ```bash
    uvicorn main:app --reload
    ```
    The backend should be accessible at `http://127.0.0.1:8000`.

7.  **Deploy to Google Cloud Run (for production/live use):**
    * Ensure Google Cloud SDK is installed and configured (`gcloud auth login`, `gcloud config set project your-gcp-project-id`).
    * Navigate to your backend directory.
    * Build and deploy your service:
        ```bash
        gcloud run deploy langbridge-ai-backend --source . --region us-central1 --allow-unauthenticated --platform managed
        ```
        (Adjust `langbridge-ai-backend` and `us-central1` as needed. Ensure your service account has the necessary permissions for Translation, Speech, and Vision APIs).
    * Note down the **Service URL** provided by Cloud Run. This will be your backend API endpoint.

### 2. Frontend Setup (Android App)

**(Assuming your Android app code is in the root `LangbridgAI-Android-App/` directory)**

1.  **Open the project in Android Studio:**
    * Launch Android Studio.
    * Select "Open an existing Android Studio project" and navigate to the `LangbridgAI-Android-App` directory.
2.  **Update Gradle:** Android Studio might prompt you to update Gradle. Accept the update to the recommended version (currently 8.10 as per `gradle-wrapper.properties`).
3.  **Configure Google Sign-In:**
    * In Android Studio, open `app/src/main/res/values/strings.xml`.
    * Replace `YOUR_WEB_CLIENT_ID` with the **Web client ID** you obtained from the Google Cloud Console (under APIs & Services -> Credentials, type "Web application").
        ```xml
        <string name="default_web_client_id">YOUR_WEB_CLIENT_ID</string>
        ```
    * Ensure your Android OAuth Client ID is also correctly configured in GCP, matching your app's package name (`com.example.langbridgai`) and SHA-1 fingerprint.
4.  **Set Backend API Base URL:**
    * Open `app/src/main/java/com/example/langbridgai/network/RetrofitClient.kt`.
    * Update the `BASE_URL` to point to your deployed FastAPI backend's URL (or your local FastAPI URL if testing locally).
        ```kotlin
        // Example: If deployed on Google Cloud Run
        private const val BASE_URL = "YOUR_CLOUD_RUN_SERVICE_URL_HERE/"
        // Example: If running locally
        // private const val BASE_URL = "[http://10.0.2.2:8000/](http://10.0.2.2:8000/)" // For AVD emulator to access host machine localhost
        ```
5.  **Clean and Rebuild:**
    * In Android Studio, go to `Build > Clean Project`, then `Build > Rebuild Project`.
    * Alternatively, from your project's root in PowerShell/Terminal:
        ```bash
        .\gradlew.bat clean
        .\gradlew.bat assembleDebug
        ```
6.  **Run the app:**
    * Connect your Android device or start an emulator.
    * Click the "Run" button in Android Studio, or execute `.\gradlew.bat installDebug` from the terminal.
    * ## 🚀 Usage

1.  **Login:**
    * Use the dummy credentials (`demo@demo.com`, `demo`) or sign in with your Google Account.
2.  **Select Languages:**
    * On the main screen, choose your "From" and "To" languages using the dropdown spinners at the top.
3.  **Translate Text:**
    * Navigate to the "Text Translate" tab (keyboard icon).
    * Enter text in the input field and tap "Translate".
    * Copy the translated text using the "Copy Translation" button.
4.  **Translate Speech:**
    * Go to the "Speech Translate" tab (microphone icon).
    * Tap the microphone button and speak clearly.
    * The transcribed text will appear, followed by its translation.
    * Copy the translated speech using the "Copy Translation" button.
5.  **Translate Images:**
    * Switch to the "Image Translate" tab (camera icon).
    * "Upload Image" from your gallery or use "Live Camera" to take a new picture.
    * Text extracted via OCR will appear, followed by its translation.
    * Copy the translated text using the "Copy Translation" button.
6.  **View History:**
    * Navigate to the "Account" tab (person icon).
    * Tap "Translation History" to see a list of your past translations.
7.  **Logout:**
    * From the "Account" tab, tap "Logout" to end your session.

## 📸 Screenshots / Demo

*(Placeholder: Add screenshots or a GIF/video demonstrating the app's key features here)*

## 🤝 Contributing

Contributions are welcome! If you find a bug or have a feature request, please open an issue on the GitHub repository.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
