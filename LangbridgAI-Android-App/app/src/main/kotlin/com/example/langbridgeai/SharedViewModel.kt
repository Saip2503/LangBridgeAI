package com.example.langbridgai

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    private val _fromLanguageCode = MutableLiveData<String>("en") // Default to English
    val fromLanguageCode: LiveData<String> = _fromLanguageCode

    private val _toLanguageCode = MutableLiveData<String>("hi") // Default to hindi
    val toLanguageCode: LiveData<String> = _toLanguageCode

    fun setFromLanguage(code: String) {
        _fromLanguageCode.value = code
    }

    fun setToLanguage(code: String) {
        _toLanguageCode.value = code
    }
}
