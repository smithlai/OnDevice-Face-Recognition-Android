package com.ml.shubham0204.facenet_android.presentation.screens.preference

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import org.koin.android.annotation.KoinViewModel
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt


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
    val detectionTimeMs by preferencesManager.detectionDelay.collectAsState()
    val detectionTimeoutMs by preferencesManager.detectionTimeout.collectAsState()
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
            Text("Detection Confidence")
            Slider(
                value = detectionConfidence,
                onValueChange = {
                    val roundedValue = ((it / 0.05f).roundToInt() * 0.05f) // 四捨五入到最近的 0.05
                    preferencesManager.updateDetectionConfidence(roundedValue)
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
            Text("Detection Delay")
            Slider(
                value = detectionTimeMs / 1000f,
                onValueChange = {
                    val roundedValue = Math.round(it)
                    preferencesManager.updateDetectionDelay((roundedValue*1000L).toLong())
                },
                valueRange = 0f..5f, // 秒的範圍
                steps = 4 // 6 - 2 = 4 步長
            )
            Text(
                text = String.format("${detectionTimeMs / 1000}"),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            //-----------
            Text("Detection Timeout")
            Slider(
                value = detectionTimeoutMs / 1000f,
                onValueChange = {
                    val roundedValue = Math.round(it)
                    preferencesManager.updateDetectionTimeout((roundedValue*1000L).toLong())
                },
                valueRange = 10f..120f, // 秒的範圍
                steps = 10 // 12 - 2 = 10 步長
            )
            Text(
                text = String.format("${detectionTimeoutMs / 1000}"),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}



