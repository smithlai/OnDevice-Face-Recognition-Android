package com.ml.shubham0204.facenet_android

import android.content.Context
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single


@Single
class PreferenceManager(context: Context) {
    private val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

    private val _detectionConfidence = MutableStateFlow(
        prefs.getFloat("detectionConfidence", 0.5f)
    )
    val detectionConfidence = _detectionConfidence.asStateFlow()

    fun updateDetectionConfidence(value: Float) {
        prefs.edit().putFloat("detectionConfidence", value).apply()
        _detectionConfidence.value = value
    }
//    val detectionTimeoutms = mutableStateOf(prefs.getLong("detectionTimeoutms", BuildConfig.FACE_DETECTION_TIMEOUT))
//    val detectionDelayms = mutableStateOf(prefs.getLong("detectionDelayms", BuildConfig.FACE_DETECTION_DELAY))
//
//    fun updateFloat(key:String, f:Float){
//        prefs.edit().putFloat(key, f).apply()
//    }
//    fun updateLong(key:String, l:Long){
//        prefs.edit().putLong(key, l).apply()
//    }

}
