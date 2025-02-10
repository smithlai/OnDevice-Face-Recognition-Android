package com.ml.shubham0204.facenet_android.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import com.ml.shubham0204.facenet_android.data.FaceImageRecord
import com.ml.shubham0204.facenet_android.data.ImagesVectorDB
import com.ml.shubham0204.facenet_android.data.RecognitionMetrics
import com.ml.shubham0204.facenet_android.domain.embeddings.FaceNet
import com.ml.shubham0204.facenet_android.domain.face_detection.FaceSpoofDetector
import com.ml.shubham0204.facenet_android.domain.face_detection.MediapipeFaceDetector
import com.ml.shubham0204.facenet_android.presentation.components.setProgressDialogText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.time.DurationUnit
import kotlin.time.measureTimedValue
import org.koin.core.annotation.Single
import java.io.File

@Single
class ImageVectorUseCase(
    private val context: Context,
    private val mediapipeFaceDetector: MediapipeFaceDetector,
    private val faceSpoofDetector: FaceSpoofDetector,
    private val imagesVectorDB: ImagesVectorDB,
    private val faceNet: FaceNet//MobileFaceNet
) {
    companion object{
        const val IMAGE_DIR = "PersonImages"
        const val CACHE_DIR = "PersonImagesCache"
    }
    val _latestFaceRecognitionResult = MutableStateFlow<List<FaceRecognitionResult>>(emptyList())
    val latestFaceRecognitionResult = _latestFaceRecognitionResult.asStateFlow()
    data class FaceRecognitionResult(
        val croppedFace:Bitmap,
        val personID: Long,
        val boundingBox: Rect,
        val spoofResult: FaceSpoofDetector.FaceSpoofResult? = null,
        val cosineSimilarity: Float
    )

    suspend fun addImage(personID: Long, imageUri: Uri, isFromCache: Boolean): Result<Boolean> {
        // 如果是從快取中獲取，直接使用 imageUri 對應的 Bitmap
        val croppedFace: Bitmap = if (isFromCache) {
            try {
                // 從 Uri 加載 Bitmap
                val inputStream = context.contentResolver.openInputStream(imageUri)
                    ?: return Result.failure(Exception("Failed to open input stream for Uri"))
                BitmapFactory.decodeStream(inputStream).also {
                    inputStream.close()
                }
            } catch (e: Exception) {
                return Result.failure(Exception("Failed to decode Bitmap from Uri: $e"))
            }
        } else {
            // 使用 Mediapipe 進行臉部檢測並裁剪圖片
            val faceDetectionResult = mediapipeFaceDetector.getCroppedFace(imageUri)
            if (!faceDetectionResult.isSuccess) {
                return Result.failure(faceDetectionResult.exceptionOrNull()!!)
            }
            faceDetectionResult.getOrNull()
                ?: return Result.failure(Exception("Cropped face is null"))
        }

        // 建立以 PersonID 命名的資料夾
        val personFolder = createPersonImageFolder(personID)
            ?: return Result.failure(Exception("Failed to create folder for PersonID: $personID"))

        // 定義儲存路徑
        val imageFile = File(personFolder, "${System.currentTimeMillis()}.png")

        return try {
            // 儲存裁剪後的圖片
            imageFile.outputStream().use { outputStream ->
                croppedFace.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }

            // 計算臉部嵌入向量
            val embedding = faceNet.getFaceEmbedding(croppedFace)

            // 將記錄新增到資料庫
            imagesVectorDB.addFaceImageRecord(
                FaceImageRecord(
                    personID = personID,
                    imagePath = imageFile.absolutePath,
                    faceEmbedding = embedding
                )
            )
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }



    // From the given frame, return the name of the person by performing
    // face recognition
    suspend fun getNearestPersonName(
        frameBitmap: Bitmap,
        confidence_threshold: Float
    ): Pair<RecognitionMetrics?, List<FaceRecognitionResult>> {
        // Perform face-detection and get the cropped face as a Bitmap
        val (faceCropResults, t1) =
            measureTimedValue { mediapipeFaceDetector.getAllCroppedFaces(frameBitmap) }
        val faceRecognitionResults = ArrayList<FaceRecognitionResult>()
        var avgT2 = 0L
        var avgT3 = 0L
        var avgT4 = 0L

        for (cropResult in faceCropResults) {
            // Get the embedding for the cropped face (query embedding)
            val (croppedBitmap, boundingBox) = cropResult
            val (embedding, t2) = measureTimedValue { faceNet.getFaceEmbedding(croppedBitmap) }
            avgT2 += t2.toLong(DurationUnit.MILLISECONDS)
            // Perform nearest-neighbor search
            val (recognitionResults, t3) =
                measureTimedValue { imagesVectorDB.getNearestEmbeddingPersonNames(embedding, 10) }
            avgT3 += t3.toLong(DurationUnit.MILLISECONDS)

            val spoofResult = faceSpoofDetector.detectSpoof(frameBitmap, boundingBox)
            avgT4 += spoofResult.timeMillis

            if (recognitionResults.isEmpty()) {
                faceRecognitionResults.add(FaceRecognitionResult(croppedBitmap, 0, boundingBox, spoofResult, 0f))
            }else {
                var bestRecognitionResult:Pair<FaceImageRecord?, Float> = null to -1f

                // 遍歷 recognitionResults
                for (recognitionResult in recognitionResults) {
                    val distance = cosineDistance(embedding, recognitionResult.faceEmbedding)
                    if (distance > bestRecognitionResult.second && distance > confidence_threshold) {
                        bestRecognitionResult = recognitionResult to distance
                    }
                }

                faceRecognitionResults.add(
                    FaceRecognitionResult(
                        croppedBitmap,
                        bestRecognitionResult.first?.personID ?: 0,
                        boundingBox,
                        spoofResult,
                        if (bestRecognitionResult.first == null) 0f else bestRecognitionResult.second
                    )
                )

            }
        }
        val metrics =
            if (faceCropResults.isNotEmpty()) {
                RecognitionMetrics(
                    timeFaceDetection = t1.toLong(DurationUnit.MILLISECONDS),
                    timeFaceEmbedding = avgT2 / faceCropResults.size,
                    timeVectorSearch = avgT3 / faceCropResults.size,
                    timeFaceSpoofDetection = avgT4 / faceCropResults.size
                )
            } else {
                null
            }
        _latestFaceRecognitionResult.value=faceRecognitionResults
        return Pair(metrics, faceRecognitionResults)
    }

    // -1 ~ +1
    private fun cosineDistance(x1: FloatArray, x2: FloatArray): Float {
        var mag1 = 0.0f
        var mag2 = 0.0f
        var product = 0.0f
        for (i in x1.indices) {
            mag1 += x1[i].pow(2)
            mag2 += x2[i].pow(2)
            product += x1[i] * x2[i]
        }
        mag1 = sqrt(mag1)
        mag2 = sqrt(mag2)
        return product / (mag1 * mag2)
    }

    fun getFaceImageRecordsByPersonID(personID: Long): List<FaceImageRecord> {
        return imagesVectorDB.getFaceImageRecordsByPersonID(personID)
    }



    fun removeImages(personID: Long) {
        imagesVectorDB.removeFaceRecordsWithPersonID(personID)
    }

    // 統一管理 PersonImages 路徑

    private fun getPersonImageFolder(personId: Long): File {
        return File(File(context.filesDir, IMAGE_DIR), personId.toString())
    }

    // 統一管理 PersonImages 路徑
    private fun getBaseCacheFolder(): File {
        return File(context.cacheDir, CACHE_DIR)
    }
    // 統一管理 PersonImagesCache 路徑
    private fun getPersonCacheFolder(personId: Long): File {
        return File(getBaseCacheFolder(), personId.toString())
    }

    fun createPersonImageFolder(personId: Long): File? {
        val personFolder = getPersonImageFolder(personId)
        if (!personFolder.exists() && !personFolder.mkdirs()) {
            Log.e("CreateFolder", "Failed to create folder for PersonID: $personId")
            return null
        }
        return personFolder
    }
    fun createBaseCacheFolder(): File? {
        val cacheFolder = getBaseCacheFolder()
        if (!cacheFolder.exists() && !cacheFolder.mkdirs()) {
            Log.e("CreateFolder", "Failed to create base cache folder")
            return null
        }
        return cacheFolder
    }
    fun createPersonCacheFolder(personId: Long): File? {
        val cacheFolder = getPersonCacheFolder(personId)
        if (!cacheFolder.exists() && !cacheFolder.mkdirs()) {
            Log.e("CreateFolder", "Failed to create cache folder for PersonID: $personId")
            return null
        }
        return cacheFolder
    }
    fun removePersonImageFolder(personId: Long) {
        val personFolder = getPersonImageFolder(personId)
        if (personFolder.exists()) {
            val isDeleted = personFolder.deleteRecursively()
            if (!isDeleted) {
                setProgressDialogText("Failed to delete images folder for PersonID: $personId")
            }
        } else {
            setProgressDialogText("No folder found for PersonID: $personId")
        }
    }

    fun removePersonCacheFolder(personId: Long) {
        val cacheFolder = getPersonCacheFolder(personId)
        if (cacheFolder.exists()) {
            val isDeleted = cacheFolder.deleteRecursively()
            if (!isDeleted) {
                Log.e("DeleteFolder", "Failed to delete cache folder for PersonID: $personId")
            }
        }
    }

}
