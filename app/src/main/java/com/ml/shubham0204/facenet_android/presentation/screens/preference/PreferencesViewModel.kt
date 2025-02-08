package com.ml.shubham0204.facenet_android.presentation.screens.preference

import androidx.lifecycle.ViewModel
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class PreferencesViewModel(
    val preferencesManager: PreferenceManager
) : ViewModel() {
}