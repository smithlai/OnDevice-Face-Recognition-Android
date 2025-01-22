package com.ml.shubham0204.facenet_android.presentation.screens.face_list

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.text.format.DateUtils
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.rememberImagePainter
import com.ml.shubham0204.facenet_android.R
import com.ml.shubham0204.facenet_android.data.FaceImageRecord
import com.ml.shubham0204.facenet_android.data.PersonRecord
import com.ml.shubham0204.facenet_android.domain.ImageVectorUseCase
import com.ml.shubham0204.facenet_android.presentation.components.AppAlertDialog
import com.ml.shubham0204.facenet_android.presentation.components.createAlertDialog
import com.ml.shubham0204.facenet_android.presentation.screens.add_face.AddFaceScreenViewModel
import com.ml.shubham0204.facenet_android.presentation.theme.FaceNetAndroidTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceListScreen(
    onNavigateBack: (() -> Unit),
    onAddFaceClick: (() -> Unit),
    onFaceItemClick: (PersonRecord) -> Unit
) {
    val context = LocalContext.current
    val viewModel: FaceListScreenViewModel = koinViewModel()
    val addFaceViewModel: AddFaceScreenViewModel = koinViewModel()

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { selectedUri ->
            importImages(context, addFaceViewModel, selectedUri)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { selectedUri ->
            exportImages(context, selectedUri)
        }
    }

    var isMenuExpanded by remember { mutableStateOf(false) }

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
                    actions = {
                        IconButton(onClick = { isMenuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Menu"
                            )
                        }

                        // 下拉菜單
                        DropdownMenu(
                            expanded = isMenuExpanded,
                            onDismissRequest = { isMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export Images") },
                                onClick = {
                                    isMenuExpanded = false
                                    exportLauncher.launch(null)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = null
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Import Images") },
                                onClick = {
                                    isMenuExpanded = false
                                    importLauncher.launch(arrayOf("application/zip"))
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.InstallMobile,
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = onAddFaceClick) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add a new face")
                }
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                ScreenUI(viewModel, onFaceItemClick)
                    AppAlertDialog()
                }
            }
        }
}

private fun exportImages(context: Context, selectedUri: Uri) {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    CoroutineScope(Dispatchers.IO).launch {
        val imageDir = File(context.filesDir, ImageVectorUseCase.IMAGE_DIR)

        context.contentResolver.takePersistableUriPermission(
            selectedUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )

        // Use DocumentFile to navigate and create the file
        val documentFile = DocumentFile.fromTreeUri(context, selectedUri)
        documentFile?.let {
            val exportFile = documentFile.createFile("application/zip", "ExportedImages_$timestamp.zip")

            // Check if the file was created successfully
            exportFile?.let { file ->
                context.contentResolver.openOutputStream(file.uri)?.use { outputStream ->
                    ZipOutputStream(outputStream).use { zipOut ->
                        compressDirectory(imageDir, zipOut, imageDir.absolutePath)
                    }
                }
            }
        }
    }
}

private fun compressDirectory(dir: File, zipOut: ZipOutputStream, basePath: String) {
    dir.listFiles()?.forEach { file ->
        // 處理檔案路徑，並確保正確加入 ZIP 檔案
        val entryName = file.absolutePath.removePrefix("$basePath/")

        if (file.isDirectory) {
            // 加入目錄到 ZIP，目錄結尾加上 '/'
            val dirEntry = ZipEntry("$entryName/")
            zipOut.putNextEntry(dirEntry)
            zipOut.closeEntry()
            // 遞迴加入目錄內的檔案
            compressDirectory(file, zipOut, basePath)
        } else {
            // 加入檔案到 ZIP
            FileInputStream(file).use { fis ->
                val fileEntry = ZipEntry(entryName)
                zipOut.putNextEntry(fileEntry)
                fis.copyTo(zipOut)
                zipOut.closeEntry()
            }
        }
    }
}

// 修改 importImages 函数
private fun importImages(context: Context, addFaceViewModel: AddFaceScreenViewModel, uri: Uri) {
    CoroutineScope(Dispatchers.IO).launch {
        val cacheDir = addFaceViewModel.imageVectorUseCase.createBaseCacheFolder()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->

            ZipInputStream(inputStream).use { zipIn ->
                var entry: ZipEntry? = zipIn.nextEntry
                while (entry != null) {
                    val outFile = File(cacheDir, entry.name)

                    if (entry.isDirectory) {
                        // 如果是目錄，創建目錄
                        if (!outFile.exists()) {
                            outFile.mkdirs()
                        }
                    } else {
                        // 如果是檔案，確保父目錄存在
                        val parentDir = outFile.parentFile
                        if (parentDir != null && !parentDir.exists()) {
                            parentDir.mkdirs()
                        }
                        // 將檔案內容寫入
                        outFile.outputStream().use { fos ->
                            zipIn.copyTo(fos)
                        }
                    }

                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }

        }
        cacheDir?.listFiles()?.forEach { folder ->
            val personId = folder.name.toLongOrNull()
            if (personId != null) {
                val imageUris = folder.listFiles()?.mapNotNull { file ->
                    try {
                        file.toUri()
                    } catch (e: Exception) {
                        null // Ignore invalid URIs
                    }
                } ?: emptyList()
                addFaceViewModel.loadPersonData(personId)
                addFaceViewModel.selectedImageURIs.value = imageUris
                addFaceViewModel.updateImages()
            }
        }
    }
}

@Composable
private fun ScreenUI(
    viewModel: FaceListScreenViewModel,
    onFaceItemClick: (PersonRecord) -> Unit
) {
    val faces by viewModel.personFlow.collectAsState(emptyList())
    val firstResult: ImageVectorUseCase.FaceRecognitionResult? =
        viewModel.imageVectorUseCase.latestFaceRecognitionResult.value.getOrNull(0)

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        // 預覽圖片
        firstResult?.takeIf { result ->
            result.spoofResult?.isSpoof == false
        }?.let { result ->
            item {
                Image(
                    bitmap = result.croppedFace.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(8.dp)
                )
            }
        }

        // 分隔線
        item {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(Color.LightGray)
            )
        }

        // Face 列表
        items(faces) { face ->
            FaceListItem(
                personRecord = face,
                onRemoveFaceClick = { viewModel.removeFace(face.personID) },
                onFaceClick = { onFaceItemClick(face) },
                getFaceImageRecordsByPersonID = viewModel.imageVectorUseCase::getFaceImageRecordsByPersonID
            )
        }
    }
}

@Composable
private fun FaceListItem(
    personRecord: PersonRecord,
    onRemoveFaceClick: (() -> Unit),
    onFaceClick: (() -> Unit),
    getFaceImageRecordsByPersonID: (Long) -> List<FaceImageRecord> // 傳入方法獲取圖片記錄
) {
    val faceImageRecords = remember { mutableStateOf<List<FaceImageRecord>?>(null) }

    // 有ID觸發圖片讀取
    LaunchedEffect(personRecord.personID) {
        faceImageRecords.value = getFaceImageRecordsByPersonID(personRecord.personID)
    }

        Row(
            modifier = Modifier
                .fillMaxWidth()
            .background(Color.White)
            .clickable(onClick = onFaceClick) // 将点击事件绑定到 Row
            .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        // 縮圖或預設圖標
        if (faceImageRecords.value?.isNotEmpty() == true) {
            val firstImagePath = faceImageRecords.value?.firstOrNull()?.imagePath
            if (firstImagePath != null) {
                AsyncImage(
                    model = File(firstImagePath),
                    contentDescription = "Face Thumbnail",
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RectangleShape)
                        .background(Color.LightGray),
                    contentScale = ContentScale.Crop // 確保圖片裁剪顯示為圓形
                )
            }
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Default Face Icon",
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 信息文本
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

        // 刪除按鈕
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
