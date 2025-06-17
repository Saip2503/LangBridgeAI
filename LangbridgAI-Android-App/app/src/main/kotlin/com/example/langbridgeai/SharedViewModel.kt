package com.example.langbridgeai

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    private val _fromLanguageCode = MutableLiveData<String>("en") // Default to English
    val fromLanguageCode: LiveData<String> = _fromLanguageCode

    private val _toLanguageCode = MutableLiveData<String>("ko") // Default to Korean
    val toLanguageCode: LiveData<String> = _toLanguageCode

    private val _userName = MutableLiveData<String?>() // For storing logged-in username
    val userName: MutableLiveData<String?> = _userName

    fun setFromLanguage(code: String) {
        _fromLanguageCode.value = code
    }

    fun setToLanguage(code: String) {
        _toLanguageCode.value = code
    }

    fun setUserName(name: String?) { // Make it nullable for logout
        _userName.value = name
    }
}
