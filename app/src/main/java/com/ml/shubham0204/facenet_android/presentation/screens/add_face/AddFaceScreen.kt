package com.ml.shubham0204.facenet_android.presentation.screens.add_face

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ml.shubham0204.facenet_android.data.PersonRecord
import com.ml.shubham0204.facenet_android.presentation.components.AppProgressDialog
import com.ml.shubham0204.facenet_android.presentation.components.DelayedVisibility
import com.ml.shubham0204.facenet_android.presentation.components.hideProgressDialog
import com.ml.shubham0204.facenet_android.presentation.components.showProgressDialog
import com.ml.shubham0204.facenet_android.presentation.theme.FaceNetAndroidTheme
import org.koin.androidx.compose.koinViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFaceScreen(personID: Long, onNavigateBack: (() -> Unit)) {
    FaceNetAndroidTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(text = "Add Faces", style = MaterialTheme.typography.headlineSmall)
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = "Navigate Back"
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                val viewModel: AddFaceScreenViewModel = koinViewModel()
                ScreenUI(viewModel, personID)
                ImageReadProgressDialog(viewModel, onNavigateBack)
            }
        }
    }
}

@Composable
private fun ScreenUI(viewModel: AddFaceScreenViewModel, personID: Long) {
    val pickVisualMediaLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickMultipleVisualMedia()
        ) {
            val updatedUris = viewModel.selectedImageURIs.value.toMutableList().apply {
                addAll(it)
            }
            viewModel.selectedImageURIs.value = updatedUris
        }
    val isFixedPersonID = personID > 0
    var showPersonID by remember { mutableStateOf(personID) }
    var showWarning by remember { mutableStateOf(false) } // 控制警告訊息
    LaunchedEffect(showPersonID) {
        loadPersonData(showPersonID, viewModel)
        resetUnselectedImages(viewModel)
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        if (isFixedPersonID) {
            // Display the person ID as a non-editable Text
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Person ID: $showPersonID",
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            // Allow editing the person ID
            TextField(
                modifier = Modifier.fillMaxWidth(),
                value = showPersonID.toString(),
                onValueChange = { input ->
                    val filteredInput = input.filter { it.isDigit() }
                    val newId = filteredInput.toLongOrNull() ?: 0L
                    showPersonID = newId
                    // 檢查輸入的 ID 是否已存在
                    val isDuplicate = viewModel.personUseCase.getPersonById(newId) != null
                    showWarning = isDuplicate
//                    if (!isDuplicate) {
                        loadPersonData(newId, viewModel)
//                    }
                },
                label = { Text(text = "Enter the person's ID") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            if (showWarning) {
                Text(
                    text = "Warning: Person ID already exists!",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Button(
                enabled = (showPersonID > 0 && !showWarning),
                onClick = {
                    pickVisualMediaLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            ) {
                Icon(imageVector = Icons.Default.Photo, contentDescription = "Add photos")
                Text(text = "Add photos")
            }
            DelayedVisibility(viewModel.selectedImageURIs.value.isNotEmpty() && !showWarning) {
                Button(onClick = { viewModel.updateImages() }, enabled = showPersonID > 0 && !showWarning) {
                    Text(text = "Update Images")
                }
            }
        }
        DelayedVisibility(viewModel.selectedImageURIs.value.isNotEmpty()) {
            Text(
                text = "${viewModel.selectedImageURIs.value.size} image(s) selected",
                style = MaterialTheme.typography.labelSmall
            )
        }
        // 使用兩個 ImagesGrid，分別顯示 selectedImageURIs 和 unselectedImageURIs
        Text(text = "Selected Images", style = MaterialTheme.typography.titleSmall)
        ImagesGrid(viewModel, isUnselectedGrid = false)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Unselected Images", style = MaterialTheme.typography.titleSmall)
        ImagesGrid(viewModel, isUnselectedGrid = true)
    }
}

private fun resetUnselectedImages(viewModel: AddFaceScreenViewModel) {
    viewModel.unselectedImageURIs.value = mutableListOf() // 清空列表
    viewModel.imageVectorUseCase.latestFaceRecognitionResult.value.getOrNull(0)?.let { result ->
        val cacheFolder = viewModel.imageVectorUseCase.createBaseCacheFolder()
        val cachedFile = File(cacheFolder, "cached_face.png")
        result.croppedFace.compress(Bitmap.CompressFormat.PNG, 100, cachedFile.outputStream())
        viewModel.unselectedImageURIs.value = mutableListOf(Uri.fromFile(cachedFile))
    }
}

@Composable
private fun ImagesGrid(viewModel: AddFaceScreenViewModel, isUnselectedGrid: Boolean = false) {
    val uris = if (isUnselectedGrid) viewModel.unselectedImageURIs.value else viewModel.selectedImageURIs.value

    LazyVerticalGrid(columns = GridCells.Fixed(2)) {
        items(uris) { uri ->
            Box(modifier = Modifier.padding(8.dp).fillMaxWidth()) {
                AsyncImage(
                    model = uri,
                    contentDescription = null,
//                    modifier = Modifier.fillMaxWidth()
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop // 確保影像充滿容器
                )
                IconButton(
                    onClick = {
                        // 更新列表，將 URI 從一個列表移到另一個
                        if (isUnselectedGrid) {
                            viewModel.unselectedImageURIs.value = viewModel.unselectedImageURIs.value.toMutableList().apply {
                                remove(uri)
                            }
                            viewModel.selectedImageURIs.value = viewModel.selectedImageURIs.value.toMutableList().apply {
                                add(uri)
                            }
                        } else {
                            viewModel.selectedImageURIs.value = viewModel.selectedImageURIs.value.toMutableList().apply {
                                remove(uri)
                            }
                            viewModel.unselectedImageURIs.value = viewModel.unselectedImageURIs.value.toMutableList().apply {
                                add(uri)
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = if (isUnselectedGrid) Icons.Default.Add else Icons.Default.Close,
                        contentDescription = if (isUnselectedGrid) "Add image" else "Remove image",
                        tint = if (isUnselectedGrid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}




@Composable
private fun ImageReadProgressDialog(viewModel: AddFaceScreenViewModel, onNavigateBack: () -> Unit) {
    val isProcessing by remember { viewModel.isProcessingImages }
    val numImagesProcessed by remember { viewModel.numImagesProcessed }
    val context = LocalContext.current
    AppProgressDialog()
    if (isProcessing) {
        showProgressDialog()
    } else {
        if (numImagesProcessed > 0) {
            onNavigateBack()
            Toast.makeText(context, "Added to database", Toast.LENGTH_SHORT).show()
        }
        hideProgressDialog()
    }
}

// 取得PersonRecord
private fun loadPersonData(personID: Long, viewModel: AddFaceScreenViewModel) {
    
    viewModel.personUseCase.getPersonById(personID)?.let { personRecord ->
        
        viewModel.personRecordState.value = personRecord
    } ?: run {
        
        viewModel.personRecordState.value = PersonRecord(personID)  // 若找不到則設為空記錄
    }
    // 加載與此 personID 相關的 FaceImageRecords 並更新
    viewModel.imageVectorUseCase.getFaceImageRecordsByPersonID(personID)?.let { faceImageRecords ->
        viewModel.faceImageRecord.value = faceImageRecords
        val imagePaths = faceImageRecords.map { it.imagePath }
        imagePaths.forEach { imagePath ->
            Log.d("ImageVector", "Image Path: $imagePath")
        }
//        val updatedUris = viewModel.selectedImageURIs.value.toMutableList().apply {
//            addAll(imagePaths.map { Uri.parse(it) })
//        }
        viewModel.selectedImageURIs.value = imagePaths.map { Uri.parse(it) }
    }
}
