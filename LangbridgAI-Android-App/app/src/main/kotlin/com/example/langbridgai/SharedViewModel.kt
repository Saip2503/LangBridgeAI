package com.example.langbridgai

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    private val _fromLanguageCode = MutableLiveData("en") // Default to English
    val fromLanguageCode: LiveData<String> = _fromLanguageCode

    private val _toLanguageCode = MutableLiveData("ko") // Default to Korean
    val toLanguageCode: LiveData<String> = _toLanguageCode

    private val _userName = MutableLiveData<String?>() // For storing logged-in username
    val userName: MutableLiveData<String?> = _userName

    private val _userEmail = MutableLiveData<String?>() // For storing logged-in user email
    val userEmail: MutableLiveData<String?> = _userEmail

    fun setFromLanguage(code: String) {
        _fromLanguageCode.value = code
    }

    fun setToLanguage(code: String) {
        _toLanguageCode.value = code
    }

    fun setUserName(name: String?) { // Make it nullable for logout
        _userName.value = name
    }

    fun setUserEmail(email: String?) { // Set user email
        _userEmail.value = email
    }
}
