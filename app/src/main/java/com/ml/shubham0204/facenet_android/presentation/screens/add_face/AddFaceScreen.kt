package com.ml.shubham0204.facenet_android.presentation.screens.add_face

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ml.shubham0204.facenet_android.TimeoutActivity
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val viewModel: AddFaceScreenViewModel = koinViewModel()
                ScreenUI(viewModel, personID)
                ImageReadProgressDialog(viewModel, onNavigateBack)
            }
        }
    }
    val activity = LocalContext.current as? TimeoutActivity
    LaunchedEffect (Unit){
        activity?.setupDefaultInactivityTimer()
    }
}

@Composable
private fun ScreenUI(viewModel: AddFaceScreenViewModel, initial_personID: Long) {
    val pickVisualMediaLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickMultipleVisualMedia()
        ) {
            val updatedUris = viewModel.page_selectedImageURIs.value.toMutableList().apply {
                addAll(it)
            }
            viewModel.page_selectedImageURIs.value = updatedUris
        }
    val isFixedPersonID = initial_personID > 0
    var showPersonID by remember { mutableStateOf(initial_personID) }
    var showWarning by remember { mutableStateOf(false) }
    LaunchedEffect(showPersonID) {
        val (_personRecord, _faces, _uris) = viewModel.loadPersonData(showPersonID)
        viewModel.page_selectedImageURIs.value = _uris
        resetUnselectedImages(viewModel)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            if (isFixedPersonID) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Person ID: $showPersonID",
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = showPersonID.toString(),
                    onValueChange = { input ->
                        val filteredInput = input.filter { it.isDigit() }
                        val newId = filteredInput.toLongOrNull() ?: 0L
                        showPersonID = newId
                        val isDuplicate = viewModel.personUseCase.getPersonById(newId) != null
                        showWarning = isDuplicate
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
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                DelayedVisibility( !showWarning && showPersonID > 0) {
                    Button(
                        enabled = true,//(showPersonID > 0 && !showWarning),
                        onClick = {
                            pickVisualMediaLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Photo, contentDescription = "Load Media")
                        Text(text = "Media")
                    }
                }
                DelayedVisibility( !showWarning && showPersonID > 0) {
                    Button(
                        enabled = viewModel.page_selectedImageURIs.value.isNotEmpty(),
                        onClick = { viewModel.updateImages(showPersonID, viewModel.page_selectedImageURIs.value) }
                        ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = "Save")
                        Text(text = "Save")
                    }
                }
            }
        }

        item {
            DelayedVisibility(viewModel.page_selectedImageURIs.value.isNotEmpty()) {
                Text(
                    text = "${viewModel.page_selectedImageURIs.value.size} image(s) selected",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        item {
            Text(text = "Selected Images", style = MaterialTheme.typography.headlineSmall)
        }

        item {
            ImagesGridFixed(
                uris = viewModel.page_selectedImageURIs.value,
                onImageAction = { uri ->
                    viewModel.page_selectedImageURIs.value = viewModel.page_selectedImageURIs.value.toMutableList().apply {
                        remove(uri)
                    }
                    viewModel.page_unselectedImageURIs.value = viewModel.page_unselectedImageURIs.value.toMutableList().apply {
                        add(uri)
                    }
                },
                isUnselectedGrid = false
            )
        }

        item {
            Text(text = "Unselected Images", style = MaterialTheme.typography.headlineSmall)
        }

        item {
            ImagesGridFixed(
                uris = viewModel.page_unselectedImageURIs.value,
                onImageAction = { uri ->
                    viewModel.page_unselectedImageURIs.value = viewModel.page_unselectedImageURIs.value.toMutableList().apply {
                        remove(uri)
                    }
                    viewModel.page_selectedImageURIs.value = viewModel.page_selectedImageURIs.value.toMutableList().apply {
                        add(uri)
                    }
                },
                isUnselectedGrid = true
            )
        }
    }
}

@Composable
private fun ImagesGridFixed(
    uris: List<Uri>,
    onImageAction: (Uri) -> Unit,
    isUnselectedGrid: Boolean
) {
    val gridState = rememberLazyGridState()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        state = gridState,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),

        //java.lang.IllegalStateException: Vertically scrollable component was measured with an infinity maximum height constraints, which is disallowed.
        modifier = Modifier.heightIn(min = 100.dp, max = 400.dp) // 設定最小和最大高度
    ) {
        items(uris) { uri ->
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .aspectRatio(1f)
                    .fillMaxWidth()
            ) {
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                IconButton(
                    onClick = { onImageAction(uri) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp) // 確保背景大小與 Icon 尺寸匹配
                            .background(
                                color = Color.Black.copy(alpha = 0.7f), // 半透明背景色
                                shape = CircleShape // 圓形背景
                            ),
                        contentAlignment = Alignment.Center // 將 Icon 置於背景中央
                    ) {
                        Icon(
                            imageVector = if (isUnselectedGrid) Icons.Filled.Add else Icons.Filled.Close,
                            contentDescription = if (isUnselectedGrid) "Add image" else "Remove image",
                            tint = if (isUnselectedGrid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }

                }
            }
        }
    }
}

private fun resetUnselectedImages(viewModel: AddFaceScreenViewModel) {
    viewModel.page_unselectedImageURIs.value = mutableListOf() // 清空列表
    viewModel.imageVectorUseCase.latestFaceRecognitionResult.value.getOrNull(0)?.takeIf {
        it.spoofResult?.isSpoof == false
    }?.let { result ->
        val cacheFolder = viewModel.imageVectorUseCase.createBaseCacheFolder()
        val cachedFile = File(cacheFolder, "cached_face.png")
        result.croppedFace.compress(Bitmap.CompressFormat.PNG, 100, cachedFile.outputStream())
        viewModel.page_unselectedImageURIs.value = mutableListOf(Uri.fromFile(cachedFile))
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


