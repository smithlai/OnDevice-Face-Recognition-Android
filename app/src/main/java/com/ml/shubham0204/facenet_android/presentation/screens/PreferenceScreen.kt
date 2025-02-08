package com.ml.shubham0204.facenet_android.presentation.screens

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.ml.shubham0204.facenet_android.PreferenceManager
import com.ml.shubham0204.facenet_android.domain.ImageVectorUseCase
import com.ml.shubham0204.facenet_android.domain.PersonUseCase
import com.ml.shubham0204.facenet_android.presentation.screens.detect_screen.DetectScreenViewModel
import org.koin.android.annotation.KoinViewModel
import org.koin.androidx.compose.koinViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(
    onNavigateBack: () -> Unit,
) {
//    var darkModeEnabled by remember {
//        mutableStateOf(preferenceManager.darkMode)
//    }
    val viewModel: PreferencesViewModel = koinViewModel()
    val preferencesManager = viewModel.preferencesManager
    val detectionConfidence by preferencesManager.detectionConfidence.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Preferences") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {

//            Spacer(modifier = Modifier.height(16.dp))

            // Detection Confidence Threshold Slider
            Text("Detection Confidence Threshold")
            Slider(
                value = detectionConfidence,
                onValueChange = {
                    preferencesManager.updateDetectionConfidence(it)
                },
                valueRange = 0.5f..1.0f,
                steps = 9   // 11 - 2(start/end) = 9
            )
            Text(
                text = String.format("%.2f", detectionConfidence),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(16.dp))
            //-----------
//            Text("Detection Delay Threshold")
//            Slider(
//                value = (detectionDelayms / 1000).toFloat(), // 將毫秒轉換為秒
//                onValueChange = { newValue ->
//                    Log.e("aaaa", "$newValue")
//                    detectionDelayms = (newValue * 1000).toLong() // 將秒轉換回毫秒
//                },
//                valueRange = 1f..10f, // 秒的範圍
//                steps = 8 // 10 - 2 = 8 步長
//            )
//            Text(
//                text = String.format("${detectionDelayms / 1000}"),
//                modifier = Modifier.align(Alignment.CenterHorizontally)
//            )
        }
    }
}


@KoinViewModel
class PreferencesViewModel(
    val preferencesManager: PreferenceManager
) : ViewModel() {
    fun aaa(){
        preferencesManager.detectionConfidence
    }
}
