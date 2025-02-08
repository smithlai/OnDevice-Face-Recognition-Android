package com.ml.shubham0204.facenet_android.presentation.screens.preference

import android.content.Context
import com.ml.shubham0204.facenet_android.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single


@Single
class PreferenceManager(context: Context) {
    private val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

    // Detection confidence
    private val _detectionConfidence = MutableStateFlow(
        prefs.getFloat("detectionConfidence", BuildConfig.FACE_DETECTION_DISTANCE)
    )
    val detectionConfidence = _detectionConfidence.asStateFlow()

    fun updateDetectionConfidence(value: Float) {
        prefs.edit().putFloat("detectionConfidence", value).apply()
        _detectionConfidence.value = value
    }
    // Detection time
    private val _detectionDelay = MutableStateFlow(
        prefs.getLong("detectionDelay", BuildConfig.FACE_DETECTION_DELAY)
    )
    val detectionDelay = _detectionDelay.asStateFlow()

    fun updateDetectionDelay(ms: Long) {
        prefs.edit().putLong("detectionDelay", ms).apply()
        _detectionDelay.value = ms
    }

    // Detection timeout
    private val _detectionTimeout = MutableStateFlow(
        prefs.getLong("detectionTimeout", BuildConfig.FACE_DETECTION_TIMEOUT)
    )
    val detectionTimeout = _detectionTimeout.asStateFlow()

    fun updateDetectionTimeout(ms: Long) {
        prefs.edit().putLong("detectionTimeout", ms).apply()
        _detectionTimeout.value = ms
    }
//    // Auto Close time
//    private val _inactiveTimeout = MutableStateFlow(
//        prefs.getLong("inactiveTimeout", BuildConfig.INACTIVITY_TIMEOUT)
//    )
//    val inactiveTimeout = _inactiveTimeout.asStateFlow()
//
//    fun updateInactiveTimeout(ms: Long) {
//        prefs.edit().putLong("inactiveTimeout", ms).apply()
//        _inactiveTimeout.value = ms
//    }
}
