package com.ml.shubham0204.facenet_android.presentation.screens.detect_screen

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector

import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import com.ml.shubham0204.facenet_android.R
import com.ml.shubham0204.facenet_android.domain.ImageVectorUseCase
import com.ml.shubham0204.facenet_android.presentation.components.AppAlertDialog
import com.ml.shubham0204.facenet_android.presentation.components.DelayedVisibility
import com.ml.shubham0204.facenet_android.presentation.components.FaceDetectionOverlay
import com.ml.shubham0204.facenet_android.presentation.components.createAlertDialog
import com.ml.shubham0204.facenet_android.presentation.theme.FaceNetAndroidTheme
import org.koin.androidx.compose.koinViewModel

private val cameraPermissionStatus = mutableStateOf(false)
private val cameraFacing = mutableIntStateOf(CameraSelector.LENS_FACING_BACK)
private lateinit var cameraPermissionLauncher: ManagedActivityResultLauncher<String, Boolean>

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetectScreen(from_external: Boolean, add_face: Boolean,onOpenFaceListClick: (() -> Unit)) {
    FaceNetAndroidTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(),
                    title = {
                        Text(
                            text = stringResource(id = R.string.app_name),
                            style = MaterialTheme.typography.headlineSmall
                        )
                    },
                    actions = {
                        if (from_external && !add_face ){

                        }else{
                            IconButton(onClick = onOpenFaceListClick) {
                                Icon(
                                    imageVector = Icons.Default.Face,
                                    contentDescription = "Open Face List"
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                if (cameraFacing.intValue == CameraSelector.LENS_FACING_BACK) {
                                    cameraFacing.intValue = CameraSelector.LENS_FACING_FRONT
                                } else {
                                    cameraFacing.intValue = CameraSelector.LENS_FACING_BACK
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = "Switch Camera"
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) { ScreenUI(from_external, add_face) }
        }
    }
}

@Composable
private fun ScreenUI(from_external:Boolean, add_face:Boolean) {
    val viewModel: DetectScreenViewModel = koinViewModel()

    Box {
        Camera(viewModel)
        DelayedVisibility(viewModel.getNumPeople() > 0) {
            val metrics by remember{ viewModel.faceDetectionMetricsState }
            val faceDetectionResults = viewModel.imageVectorUseCase.latestFaceRecognitionResult
            val numPeople = viewModel.getNumPeople()
            Column {
                Text(
                    text = "Recognition on $numPeople face(s)",
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.weight(1f))
                metrics?.let {
                    Text(
                        text = "face detection: ${it.timeFaceDetection} ms" +
                                "\nface embedding: ${it.timeFaceEmbedding} ms" +
                                "\nvector search: ${it.timeVectorSearch} ms\n" +
                                "spoof detection: ${it.timeFaceSpoofDetection} ms",
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        textAlign = TextAlign.Center
                    )
                }
                val activity = LocalContext.current as? Activity
                var lastPersonID: Long? by remember { mutableStateOf(null) }
                var lastTimestamp: Long by remember { mutableStateOf(0L) }
                val stableDetectionDelay: Long = 1000L // 1 seconds
                var currentResult: ImageVectorUseCase.FaceRecognitionResult? by remember { mutableStateOf(null) }

                if (from_external && !add_face && currentResult == null) { // 僅當對話框未顯示時執行背景邏輯
                    if (faceDetectionResults.value.size == 1) {
                        faceDetectionResults.value.getOrNull(0)?.takeIf {
                            it.spoofResult?.isSpoof != true && it.personID > 0
                        }?.let { result ->
                            val currentTime = System.currentTimeMillis()
                            if (result.personID == lastPersonID) {
                                if (currentTime - lastTimestamp >= stableDetectionDelay) {
                                    currentResult = result // 儲存結果並顯示對話框
                                }
                            } else {
                                lastPersonID = result.personID
                                lastTimestamp = currentTime
                            }
                        } ?: run {
                            lastPersonID = null
                            lastTimestamp = System.currentTimeMillis()
                        }
                    } else {
                        lastPersonID = null
                        lastTimestamp = System.currentTimeMillis()
                    }
                }

                // 確認對話框
                currentResult?.let { savedResult ->
                    showConfirmationDialog(
                        context = LocalContext.current,
                        faceRecognitionResult = savedResult, // 顯示裁剪後的人臉
                        onConfirm = {
                            // 用戶確認
                            currentResult = null // 關閉對話框

                            activity?.setResult(Activity.RESULT_OK, Intent().apply {
                                putExtra("user_id", savedResult.personID)
                            })
                            activity?.finish()
                        },
                        onCancel = {
                            currentResult = null // 關閉對話框
                            lastPersonID = null
                            lastTimestamp = System.currentTimeMillis()
                        }
                    )
                }



            }
        }
        DelayedVisibility(viewModel.getNumPeople() == 0L) {
            Text(
                text = "No images in database",
                color = Color.White,
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(Color.Blue, RoundedCornerShape(16.dp))
                    .padding(8.dp),
                textAlign = TextAlign.Center
            )
        }
        AppAlertDialog()
    }
}
@Composable
fun showConfirmationDialog(
    context: Context,
    faceRecognitionResult: ImageVectorUseCase.FaceRecognitionResult,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { /* Do nothing */ },
        title = {
            Text("確認身份")
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Display the detected face
                Image(
                    bitmap = faceRecognitionResult.croppedFace.asImageBitmap(),
                    contentDescription = "Detected Face",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(8.dp)
                )
                Text("是否確認為本人？(${faceRecognitionResult.personID})")
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("是")
            }
        },
        dismissButton = {
            Button(onClick = onCancel) {
                Text("否")
            }
        }
    )
}
@OptIn(ExperimentalGetImage::class)
@Composable
private fun Camera(viewModel: DetectScreenViewModel) {
    val context = LocalContext.current
    cameraPermissionStatus.value =
        ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
    val cameraFacing by remember { cameraFacing }
    val lifecycleOwner = LocalLifecycleOwner.current

    cameraPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                cameraPermissionStatus.value = true
            } else {
                camaraPermissionDialog()
            }
        }

    DelayedVisibility(cameraPermissionStatus.value) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { FaceDetectionOverlay(lifecycleOwner, context, viewModel) },
            update = { it.initializeCamera(cameraFacing) }
        )
    }
    DelayedVisibility(!cameraPermissionStatus.value) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Allow Camera Permissions\nThe app cannot work without the camera permission.",
                textAlign = TextAlign.Center
            )
            Button(
                onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(text = "Allow")
            }
        }
    }
}

private fun camaraPermissionDialog() {
    createAlertDialog(
        "Camera Permission",
        "The app couldn't function without the camera permission.",
        "ALLOW",
        "CLOSE",
        onPositiveButtonClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
        onNegativeButtonClick = {
            // TODO: Handle deny camera permission action
            //       close the app
        }
    )
}
