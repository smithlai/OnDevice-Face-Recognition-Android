package com.ml.shubham0204.facenet_android.domain.embeddings

import android.content.Context
import android.graphics.Bitmap
import com.ml.shubham0204.facenet_android.BuildConfig.FACE_EMBEDDING_DIMENSION
import java.nio.ByteBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp

@Single
class MobileFaceNet(context: Context, useGpu: Boolean = true, useXNNPack: Boolean = true) {

    // Input image size for MobileFaceNet model (InsightFace)
    private val imgSize = 112

    // Output embedding size for MobileFaceNet (192-dim)
    private val embeddingDim = FACE_EMBEDDING_DIMENSION

    private var interpreter: Interpreter
    private val imageTensorProcessor =
        ImageProcessor.Builder()
            .add(ResizeOp(imgSize, imgSize, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(127.5f, 128.0f)) // InsightFace 模型預設歸一化
            .build()

    init {
        // Initialize TFLite Interpreter
        val interpreterOptions =
            Interpreter.Options().apply {
                if (useGpu) {
                    if (CompatibilityList().isDelegateSupportedOnThisDevice) {
                        addDelegate(GpuDelegate(CompatibilityList().bestOptionsForThisDevice))
                    }
                } else {
                    numThreads = 4
                }
                useXNNPACK = useXNNPack
                useNNAPI = true
            }

        // Load InsightFace MobileFaceNet model
        interpreter = Interpreter(FileUtil.loadMappedFile(context, "mobilefacenet.tflite"), interpreterOptions)
    }

    // Gets a face embedding using InsightFace (MobileFaceNet)
    suspend fun getFaceEmbedding(image: Bitmap) =
        withContext(Dispatchers.Default) {
            return@withContext runInsightFace(convertBitmapToBuffer(image))[0]
        }

    // Run the InsightFace model
    private fun runInsightFace(inputs: Any): Array<FloatArray> {
        val modelOutputs = Array(1) { FloatArray(embeddingDim) }
        interpreter.run(inputs, modelOutputs)
        return modelOutputs
    }

    // Resize the given bitmap and convert it to a ByteBuffer
    private fun convertBitmapToBuffer(image: Bitmap): ByteBuffer {
        return imageTensorProcessor.process(TensorImage.fromBitmap(image)).buffer
    }
}
