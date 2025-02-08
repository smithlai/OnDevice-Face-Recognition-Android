package com.ml.shubham0204.facenet_android.presentation.screens.detect_screen

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.ml.shubham0204.facenet_android.BuildConfig
import com.ml.shubham0204.facenet_android.PreferenceManager
import com.ml.shubham0204.facenet_android.data.RecognitionMetrics
import com.ml.shubham0204.facenet_android.domain.ImageVectorUseCase
import com.ml.shubham0204.facenet_android.domain.PersonUseCase
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class DetectScreenViewModel(
    val personUseCase: PersonUseCase,
    val imageVectorUseCase: ImageVectorUseCase,
    val preferencesManager: PreferenceManager
) : ViewModel() {
//    var detectionScreenElapse = mutableStateOf(0L)
    var validFaceElapse = mutableStateOf(0L)
    val faceDetectionMetricsState = mutableStateOf<RecognitionMetrics?>(null)
    fun getNumPeople(): Long = personUseCase.getCount()
}
