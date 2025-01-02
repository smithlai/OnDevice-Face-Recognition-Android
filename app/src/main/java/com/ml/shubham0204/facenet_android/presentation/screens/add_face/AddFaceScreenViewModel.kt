package com.ml.shubham0204.facenet_android.presentation.screens.add_face

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import com.ml.shubham0204.facenet_android.data.FaceImageRecord
import com.ml.shubham0204.facenet_android.data.PersonRecord
import com.ml.shubham0204.facenet_android.domain.AppException
import com.ml.shubham0204.facenet_android.domain.ImageVectorUseCase
import com.ml.shubham0204.facenet_android.domain.PersonUseCase
import com.ml.shubham0204.facenet_android.presentation.components.setProgressDialogText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import java.io.File

@KoinViewModel
class AddFaceScreenViewModel(
    val context: Context,
    val personUseCase: PersonUseCase,
    val imageVectorUseCase: ImageVectorUseCase
) : ViewModel() {

//    val show_personIdState: MutableState<Long> = mutableStateOf(0)
    val personRecordState: MutableState<PersonRecord?> = mutableStateOf(null)
    val faceImageRecord: MutableState<List<FaceImageRecord>?> = mutableStateOf(null)

    val selectedImageURIs: MutableState<List<Uri>> = mutableStateOf(emptyList())

    val isProcessingImages: MutableState<Boolean> = mutableStateOf(false)
    val numImagesProcessed: MutableState<Int> = mutableIntStateOf(0)

    fun updateImages() {
        isProcessingImages.value = true
        CoroutineScope(Dispatchers.Default).launch {
            var cacheFolder: File? = null // 提前宣告 cacheFolder
            try {

                val pr = personRecordState.value?.takeIf { it.personID > 0 }
                    ?: throw Exception("Invalid PersonRecord")

                val existingPerson = personUseCase.getPersonById(pr.personID)
                personRecordState.value = existingPerson?.takeIf { it.personID != null && pr.personID > 0 }
                    ?: run {
                        personUseCase.addPerson(pr.personID, selectedImageURIs.value.size.toLong())
                        personUseCase.getPersonById(pr.personID)
                    }


                val personId = personRecordState.value?.personID!!
                // 建立目標資料夾與快取資料夾
                val personFolder = imageVectorUseCase.createPersonFolder(personId)
                    ?: throw Exception("Failed to create folder for PersonID: ${personId}")

                cacheFolder = imageVectorUseCase.createCacheFolder(personId)

                // 備份現有圖片到 Cache 資料夾
                personFolder.listFiles()?.forEach { file ->
                    val cacheFile = File(cacheFolder, file.name)
                    file.copyTo(cacheFile, overwrite = true)
                    Log.e("aaaa", "${file.path}->${cacheFile.path}, ${cacheFile.exists()}")
                    file.delete()
                }
                //先清空影像資料庫
                imageVectorUseCase.removeImages(personId)
                imageVectorUseCase.removePersonFolder(personId)

                // 新圖片處理
                selectedImageURIs.value.forEach { imageUri ->
                    val (resolvedUri, isFromCache) = resolveUri(imageUri, personFolder, cacheFolder!!)
                    imageVectorUseCase
                        .addImage(personId, resolvedUri, isFromCache)
                        .onFailure {
                            throw it // 任一失敗直接中斷並拋出異常
                        }
                        .onSuccess {
                            numImagesProcessed.value += 1
                            setProgressDialogText("Processed ${numImagesProcessed.value} image(s)")
                        }
                }

            } catch (e: Exception) {
//                // 從 Cache 資料夾還原圖片
//                val personFolder = imageVectorUseCase.createFolderForPerson(personIdState.value)
//                cacheFolder?.listFiles()?.forEach { cachedFile ->
//                    cachedFile.copyTo(File(personFolder, cachedFile.name), overwrite = true)
//                }
//                setProgressDialogText("Error updating images: ${e.message}")

            } finally {
                // 清理 Cache 資料夾
                cacheFolder?.deleteRecursively()
                isProcessingImages.value = false
            }
        }
    }

    /**
     * 將圖片 URI 解析為實際的圖片位置，若在 Person 資料夾中則改用 Cache 資料夾圖片。
     */
    private fun resolveUri(imageUri: Uri, personFolder: File, cacheFolder: File): Pair<Uri, Boolean> {
        val sourceFile = File(imageUri.path ?: "")

        return if (sourceFile.parentFile?.absolutePath == personFolder.absolutePath) {
            val cachedFile = File(cacheFolder, sourceFile.name)
            if (cachedFile.exists()) {
                Pair(Uri.fromFile(cachedFile), true)
            } else {
                Log.e("Fatal Error", "Should not BE!!!!")
                Pair(imageUri, false)
            }
        } else {
            Pair(imageUri, false)
        }
    }


}
