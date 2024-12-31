package com.ml.shubham0204.facenet_android.presentation.screens.face_list

import android.text.format.DateUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ml.shubham0204.facenet_android.data.PersonRecord
import com.ml.shubham0204.facenet_android.domain.ImageVectorUseCase
import com.ml.shubham0204.facenet_android.presentation.components.AppAlertDialog
import com.ml.shubham0204.facenet_android.presentation.components.createAlertDialog
import com.ml.shubham0204.facenet_android.presentation.theme.FaceNetAndroidTheme
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceListScreen(
    onNavigateBack: (() -> Unit),
    onAddFaceClick: (() -> Unit),
    onFaceItemClick: (PersonRecord) -> Unit
) {
    FaceNetAndroidTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(text = "Face List", style = MaterialTheme.typography.headlineSmall)
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = "Navigate Back"
                            )
                        }
                    },
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = onAddFaceClick) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add a new face")
                }
            }
        ) { innerPadding ->
            val viewModel: FaceListScreenViewModel = koinViewModel()
            Column(modifier = Modifier.padding(innerPadding)) {
                ScreenUI(viewModel, onFaceItemClick)
                AppAlertDialog()
            }
        }
    }
}

@Composable
private fun ScreenUI(
    viewModel: FaceListScreenViewModel,
    onFaceItemClick: (PersonRecord) -> Unit // 传递 PersonRecord 参数的点击事件
) {
    val faces by viewModel.personFlow.collectAsState(emptyList())
    val firstResult: ImageVectorUseCase.FaceRecognitionResult? =
        viewModel.imageVectorUseCase.latestFaceRecognitionResult.value.getOrNull(0)

    // 單一圖片顯示區域
    firstResult?.let { result ->
        Image(
            bitmap = result.croppedFace.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(8.dp)
        )
    }

    // 分隔线
    Spacer(modifier = Modifier.height(8.dp).fillMaxWidth().background(Color.LightGray))

    // Face 列表
    LazyColumn {
        items(faces) { face ->
            FaceListItem(
                personRecord = face,
                onRemoveFaceClick = { viewModel.removeFace(face.personID) },
                onFaceClick = { onFaceItemClick(face) } // 动态传递当前 FaceItem
            )
        }
    }
}

@Composable
private fun FaceListItem(
    personRecord: PersonRecord,
    onRemoveFaceClick: (() -> Unit),
    onFaceClick: (() -> Unit) // 点击事件绑定
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .clickable(onClick = onFaceClick) // 将点击事件绑定到 Row
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Text(
                text = personRecord.personID.toString(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = DateUtils.getRelativeTimeSpanString(personRecord.addTime).toString(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.DarkGray
            )
        }
        Icon(
            modifier =
            Modifier.clickable {
                createAlertDialog(
                    dialogTitle = "Remove person",
                    dialogText =
                    "Are you sure to remove this person from the database? The face for this person will not " +
                            "be detected in real-time.",
                    dialogPositiveButtonText = "Remove",
                    onPositiveButtonClick = onRemoveFaceClick,
                    dialogNegativeButtonText = "Cancel",
                    onNegativeButtonClick = {}
                )
            },
            imageVector = Icons.Default.Clear,
            contentDescription = "Remove face"
        )
        Spacer(modifier = Modifier.width(2.dp))
    }
}
