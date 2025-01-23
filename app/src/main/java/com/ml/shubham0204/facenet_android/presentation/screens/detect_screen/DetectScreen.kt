package com.ml.shubham0204.facenet_android.presentation.screens.detect_screen

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.runtime.LaunchedEffect
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
import com.ml.shubham0204.facenet_android.BuildConfig
import com.ml.shubham0204.facenet_android.R
import com.ml.shubham0204.facenet_android.TimeoutActivity
import com.ml.shubham0204.facenet_android.domain.ImageVectorUseCase
import com.ml.shubham0204.facenet_android.presentation.components.AppAlertDialog
import com.ml.shubham0204.facenet_android.presentation.components.DelayedVisibility
import com.ml.shubham0204.facenet_android.presentation.components.FaceDetectionOverlay
import com.ml.shubham0204.facenet_android.presentation.components.createAlertDialog
import com.ml.shubham0204.facenet_android.presentation.theme.FaceNetAndroidTheme
import org.koin.androidx.compose.koinViewModel
import java.io.ByteArrayOutputStream

private val cameraPermissionStatus = mutableStateOf(false)
private val cameraFacing = mutableIntStateOf(CameraSelector.LENS_FACING_FRONT)
private lateinit var cameraPermissionLauncher: ManagedActivityResultLauncher<String, Boolean>

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetectScreen(from_external: Boolean, adding_user: Boolean,onNavigateBack: (() -> Unit),onOpenFaceListClick: (() -> Unit)) {
    FaceNetAndroidTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        val context = LocalContext.current
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close App"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(),
                    title = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth() // 填滿整個可用寬度
                                .fillMaxHeight(), // 填滿整個可用高度
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Text 靠左，佔用固定空間
                            Text(
                                text = stringResource(id = R.string.app_name),
                                style = MaterialTheme.typography.headlineSmall,
//                                maxLines = 1,
//                                overflow = TextOverflow.Ellipsis,
//                                modifier = Modifier.weight(1f, fill = false) // 固定寬度，不撐滿
                            )

                            // IconButton 居中，佔用剩餘空間
                            Box(
                                modifier = Modifier
                                    .weight(1f) // 佔用剩餘空間
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (from_external && !adding_user) {

                                }else{
                                    IconButton(onClick = onOpenFaceListClick) {
                                        Icon(
                                            imageVector = Icons.Default.Face,
                                            contentDescription = "Open Face List",
                                            modifier = Modifier.size(48.dp) // 放大 Icon
                                        )
                                    }
                                }
                            }
                        }
                    },

                    actions = {
//                        IconButton(onClick = onOpenFaceListClick) {
//                            Icon(
//                                imageVector = Icons.Default.Face,
//                                contentDescription = "Open Face List",
//                                modifier = Modifier.size(48.dp) // 放大 Icon
//                            )
//                        }
//                        Spacer(modifier = Modifier.width(Dp(20f)))
////                        IconButton(
////                            onClick = {
////                                if (cameraFacing.intValue == CameraSelector.LENS_FACING_BACK) {
////                                    cameraFacing.intValue = CameraSelector.LENS_FACING_FRONT
////                                } else {
////                                    cameraFacing.intValue = CameraSelector.LENS_FACING_BACK
////                                }
////                            }
////                        ) {
////                            Icon(
////                                imageVector = Icons.Default.Cameraswitch,
////                                contentDescription = "Switch Camera",
////                                modifier = Modifier.size(48.dp) // 放大 Icon
////                            )
////                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) { ScreenUI(from_external, adding_user) }
        }
    }
}
fun setupInactivityTimer(activity: TimeoutActivity?){
    //        if (from_external) {
    activity?.setupInactivityTimer(
        {
            activity?.setResult(Activity.RESULT_CANCELED)
            activity?.finish()
        }, inactivity_timeout_ms = BuildConfig.FACE_DETECTION_TIMEOUT,
        warning_before_close_ms = BuildConfig.FACE_DETECTION_TIMEOUT / 2
    )
//        }else{
//            activity?.clearInactivityTimer()
//        }
}
@Composable
private fun ScreenUI(from_external: Boolean, adding_user: Boolean) {
    val viewModel: DetectScreenViewModel = koinViewModel()
    val activity = LocalContext.current as? TimeoutActivity
    LaunchedEffect (Unit){
        setupInactivityTimer(activity)
    }
    Box {
        Camera(viewModel)
        DelayedVisibility(viewModel.getNumPeople() > 0) {
            val metrics by remember{ viewModel.faceDetectionMetricsState }
            val faceDetectionResults = viewModel.imageVectorUseCase.latestFaceRecognitionResult
            val numPeople = viewModel.getNumPeople()
            Column {
                Text(
                    text = if (from_external){
                        if (adding_user) {
                            "臉部拍照中，按下上方笑臉圖案截圖"
                        }else{
                            "偵測臉部中，請靜止${viewModel.stableDetectionDelay/1000}秒確保人物正確"
                        }
                    }else {
                        "Recognition on $numPeople face(s)"
                    },
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
//                Spacer(modifier = Modifier.weight(1f))
//                metrics?.let {
//                    Text(
//                        text =
//                        "face detection: ${it.timeFaceDetection} ms" +
//                                "\nface embedding: ${it.timeFaceEmbedding} ms" +
//                                "\nvector search: ${it.timeVectorSearch} ms\n" +
//                                "spoof detection: ${it.timeFaceSpoofDetection} ms",
//                        color = Color.White,
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(bottom = 24.dp),
//                        textAlign = TextAlign.Center
//                    )
//                }

                var lastPersonID: Long? by remember { mutableStateOf(null) }
                var lastFaceTimestamp: Long by remember { mutableStateOf(0L) }
//                var idleStart: Long by remember { mutableStateOf(System.currentTimeMillis()) }
                var currentResult: ImageVectorUseCase.FaceRecognitionResult? by remember { mutableStateOf(null) }
//                viewModel.detectionScreenElapse.value = System.currentTimeMillis() - idleStart

//                // 添加倒數計時顯示
//                if (viewModel.detectionScreenElapse.value > 0) {
//
//                    val remainingTime = (BuildConfig.FACE_DETECTION_TIMEOUT - viewModel.detectionScreenElapse.value)
//                    if (remainingTime > 0) {
//                        Text(
//                            text = "${remainingTime / 1000}秒後自動關閉",
//                            color = Color.White,
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(top = 16.dp),
//                            textAlign = TextAlign.Center
//                        )
//                    }
//                }

//                if (viewModel.detectionScreenElapse.value > BuildConfig.FACE_DETECTION_TIMEOUT) {
//                        // 超時，關閉活動
//                        activity?.setResult(Activity.RESULT_CANCELED)
//                        activity?.finish()
//                    }


                // 偵測臉部邏輯
                if (from_external && !adding_user && currentResult == null) {
                    if (faceDetectionResults.value.size == 1) {
                        faceDetectionResults.value.getOrNull(0)?.takeIf {
                            it.spoofResult?.isSpoof != true && it.personID > 0
                        }?.let { result ->
                            val currentTime = System.currentTimeMillis()
                            if (result.personID == lastPersonID) {
                                viewModel.validFaceElapse.value = currentTime - lastFaceTimestamp
                                if (viewModel.validFaceElapse.value >= viewModel.stableDetectionDelay) {
                                    currentResult = result
                                }
                            } else {
                                lastPersonID = result.personID
                                lastFaceTimestamp = currentTime
                                viewModel.validFaceElapse.value = 0
                            }
                        } ?: run {
                            lastPersonID = null
                            lastFaceTimestamp = System.currentTimeMillis()
                            viewModel.validFaceElapse.value = 0
                        }
                    } else {
                        lastPersonID = null
                        lastFaceTimestamp = System.currentTimeMillis()
                        viewModel.validFaceElapse.value = 0
                    }
                }

                // 確認對話框
                currentResult?.let { savedResult ->
                    setupInactivityTimer(activity)
                    showConfirmationDialog(
                        context = LocalContext.current,
                        faceRecognitionResult = savedResult,
                        onConfirm = {
                            currentResult = null
                            activity?.setResult(Activity.RESULT_OK, Intent().apply {
                                putExtra("user_id", savedResult.personID)
                                val stream = ByteArrayOutputStream()
                                savedResult.croppedFace.compress(Bitmap.CompressFormat.PNG, 100, stream)
                                putExtra("login_face", stream.toByteArray())
                            })
                            activity?.finish()
                        },
                        onCancel = {
                            setupInactivityTimer(activity)
                            currentResult = null
                            lastPersonID = null
                            lastFaceTimestamp = System.currentTimeMillis()
                        }
                    )
                }
            }
        }
        DelayedVisibility(viewModel.getNumPeople() == 0L) {
            Text(
                text = "No images in database",
                color = Color.White,
                modifier = Modifier
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
