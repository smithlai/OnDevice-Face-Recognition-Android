package com.ml.shubham0204.facenet_android.presentation.screens.add_face

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.flow.firstOrNull
import org.koin.androidx.compose.koinViewModel

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
//            viewModel.selectedImageURIs.value = it
            val updatedUris = viewModel.selectedImageURIs.value.toMutableList().apply {
                addAll(it)
            }
            viewModel.selectedImageURIs.value = updatedUris
        }
    var personName by remember { viewModel.personIdState }
//    // Load data if personID is not null
//    LaunchedEffect(personID) {
//        personID?.let {
//            val personRecord = viewModel.personUseCase.getAll()
//                .firstOrNull { person -> person?.get(0)?.personID == it }
//            personRecord?.let { record ->
//                personName = record?.get(0)?.personID!! // Automatically fill the name
//            }
//        }
//    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = personName.toString(),
            onValueChange = {  input ->
                // 過濾只允許數字輸入
                val filteredInput = input.filter { it.isDigit() }
                personName = filteredInput.toLongOrNull() ?: 0L
            },
            label = { Text(text = "Enter the person's ID") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Button(
                enabled = (personName > 0),
                onClick = {
                    pickVisualMediaLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            ) {
                Icon(imageVector = Icons.Default.Photo, contentDescription = "Choose photos")
                Text(text = "Choose photos")
            }
            DelayedVisibility(viewModel.selectedImageURIs.value.isNotEmpty()) {
                Button(onClick = { viewModel.updateImages() },  enabled = personID > 0) { Text(text = "Update Images") }
            }
        }
        DelayedVisibility(viewModel.selectedImageURIs.value.isNotEmpty()) {
            Text(
                text = "${viewModel.selectedImageURIs.value.size} image(s) selected",
                style = MaterialTheme.typography.labelSmall
            )
        }
        ImagesGrid(viewModel)
    }
}

@Composable
private fun ImagesGrid(viewModel: AddFaceScreenViewModel) {
    val uris by remember { viewModel.selectedImageURIs }
    LazyVerticalGrid(columns = GridCells.Fixed(2)) {
        items(uris) { AsyncImage(model = it, contentDescription = null) }
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
