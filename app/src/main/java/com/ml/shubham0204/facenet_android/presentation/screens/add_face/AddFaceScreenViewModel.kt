package com.ml.shubham0204.facenet_android.presentation.screens.add_face

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.ml.shubham0204.facenet_android.data.FaceImageRecord
import com.ml.shubham0204.facenet_android.data.PersonRecord
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
//    val page_personRecordState: MutableState<PersonRecord?> = mutableStateOf(null)
//    val faceImageRecord: MutableState<List<FaceImageRecord>?> = mutableStateOf(null)

    val page_selectedImageURIs: MutableState<List<Uri>> = mutableStateOf(emptyList())
    val page_unselectedImageURIs: MutableState<List<Uri>> = mutableStateOf(emptyList())
    val isProcessingImages: MutableState<Boolean> = mutableStateOf(false)
    val numImagesProcessed: MutableState<Int> = mutableIntStateOf(0)

    fun updateImages(personId:Long, selectedImageURIs:List<Uri>) {
        isProcessingImages.value = true
        CoroutineScope(Dispatchers.Default).launch {
            var personCacheFolder: File? = null // 提前宣告 cacheFolder
            try {
                if ( personId <= 0) throw Exception("Invalid PersonID: $personId")


                val existingPerson = personUseCase.getPersonById(personId)?:run{
                    personUseCase.addPerson(personId, selectedImageURIs.size.toLong())
                    personUseCase.getPersonById(personId)
                }?: throw Exception("Failed to addPerson for PersonID: $personId")


                // 建立目標資料夾與快取資料夾
                val personImageFolder = imageVectorUseCase.createPersonImageFolder(personId)
                    ?: throw Exception("Failed to create folder for PersonID: $personId")

                personCacheFolder = imageVectorUseCase.createPersonCacheFolder(personId)
                    ?: throw Exception("Failed to create cache for PersonID: $personId")

                // 備份現有圖片到 Cache 資料夾
                personImageFolder.listFiles()?.forEach { file ->
                    val cacheFile = File(personCacheFolder, file.name)
                    file.copyTo(cacheFile, overwrite = true)
                    file.delete()
                }

                // 清空影像資料庫與資料夾
                imageVectorUseCase.removeImages(personId)

                imageVectorUseCase.removePersonImageFolder(personId)

                // 新圖片處理
                selectedImageURIs.forEach { imageUri ->
                    try {
                        val (resolvedUri, isFromCache) = resolveUri(imageUri, personImageFolder, personCacheFolder!!)

                        imageVectorUseCase
                            .addImage(personId, resolvedUri, isFromCache)
                            .onFailure {
                                Log.e("updateImages", "Failed to add image: ${resolvedUri}, Error: ${it.message}")
                                throw it
                            }
                            .onSuccess {
                                numImagesProcessed.value += 1
                                setProgressDialogText("Processed ${numImagesProcessed.value} image(s)")
                            }
                    } catch (e: Exception) {
                        Log.e("updateImages", "Error processing image URI: $imageUri", e)
                        throw e
                    }
                }
            } catch (e: Exception) {
                Log.e("updateImages", "Error updating images", e)
            } finally {
                // 清理 Cache 資料夾
                personCacheFolder?.deleteRecursively()
                isProcessingImages.value = false
            }
        }
    }

    /**
     * 將圖片 URI 解析為實際的圖片位置，若在 Person 資料夾中則改用 Cache 資料夾圖片。
     */
    private fun resolveUri(imageUri: Uri, personImageFolder: File, personCacheFolder: File): Pair<Uri, Boolean> {
        val sourceFile = File(imageUri.path ?: "")
        val baseCacheFolder = imageVectorUseCase.createBaseCacheFolder()
        return if (sourceFile.parentFile?.absolutePath == personImageFolder.absolutePath) {
            // 沿用原先已經切臉的圖片
            // 因為圖片資料夾會先被清空，所以暫時使用備份的cache作為來源
            val cachedFile = File(personCacheFolder, sourceFile.name)
            if (cachedFile.exists()) {
                Pair(Uri.fromFile(cachedFile), true)
            } else {

                Log.e("Fatal Error", "Should not BE!!!!")
                Pair(imageUri, false)
            }
        } else if (sourceFile.parentFile?.absolutePath!!.startsWith(baseCacheFolder?.absolutePath!!)){
            // 位於cache資料夾，但並非該使用者，表示臉部應該是切過的。通常是即時拍照截圖。 cacheImage/face.png
            // 位於cache資料夾，且位於個人資料夾中，表示可能是要匯入
            Pair(imageUri, true)
        } else {
            Pair(imageUri, false)
        }
    }

    // 取得PersonRecord
    fun loadPersonData(personID: Long):Triple<PersonRecord,List<FaceImageRecord>, List<Uri>> {
        var personRecord = personUseCase.getPersonById(personID)?.let { personRecord ->
            personRecord
        } ?: run {
            PersonRecord(personID)  // 若找不到則設為空記錄
        }
        Log.e("loadPersonData", "loadPersonData : $personID : ${personRecord.personID}")
        var imageUri:List<Uri> = emptyList()
        var faceImageRecord = imageVectorUseCase.getFaceImageRecordsByPersonID(personID)?.let { faceImageRecords ->
            val imagePaths = faceImageRecords.map { it.imagePath }
            imagePaths.forEach { imagePath ->
                Log.d("loadPersonData", "${personRecord.personID} Image Path: $imagePath")
            }
//        val updatedUris = viewModel.selectedImageURIs.value.toMutableList().apply {
//            addAll(imagePaths.map { Uri.parse(it) })
//        }
//            selectedImageURIs.value = imagePaths.map { Uri.parse(it) }
            imageUri = imagePaths.map { Uri.parse(it) }
            faceImageRecords
        }?:emptyList<FaceImageRecord>()

        return Triple<PersonRecord,List<FaceImageRecord>, List<Uri> >(personRecord, faceImageRecord, imageUri)
    }

}
