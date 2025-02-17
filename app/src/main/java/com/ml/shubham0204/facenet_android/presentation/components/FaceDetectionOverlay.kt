package com.ml.shubham0204.facenet_android.presentation.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.FrameLayout
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.graphics.toRectF
import androidx.core.view.doOnLayout
import androidx.lifecycle.LifecycleOwner
import com.ml.shubham0204.facenet_android.presentation.screens.detect_screen.DetectScreenViewModel
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min

@SuppressLint("ViewConstructor")
@ExperimentalGetImage
class FaceDetectionOverlay(
    private val lifecycleOwner: LifecycleOwner,
    private val context: Context,
    private val viewModel: DetectScreenViewModel
) : FrameLayout(context) {

    private var overlayWidth: Int = 0
    private var overlayHeight: Int = 0

    private var imageTransform: Matrix = Matrix()
    private var boundingBoxTransform: Matrix = Matrix()
    private var isImageTransformedInitialized = false
    private var isBoundingBoxTransformedInitialized = false

    private lateinit var frameBitmap: Bitmap
    private var isProcessing = false
    private var cameraFacing: Int = CameraSelector.LENS_FACING_FRONT
    private lateinit var boundingBoxOverlay: BoundingBoxOverlay
    private lateinit var previewView: PreviewView

    var predictions: Array<Prediction> = arrayOf()

    init {
        initializeCamera(cameraFacing)
        doOnLayout {
            overlayHeight = it.measuredHeight
            overlayWidth = it.measuredWidth
        }
    }

    fun initializeCamera(cameraFacing: Int) {
        this.cameraFacing = cameraFacing
        this.isImageTransformedInitialized = false
        this.isBoundingBoxTransformedInitialized = false
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val previewView = PreviewView(context)
        val executor = ContextCompat.getMainExecutor(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val availableCameras = cameraProvider.availableCameraInfos
            val cameraSelector = if (availableCameras.any { it.lensFacing == cameraFacing }) {
                // 如果指定的 cameraFacing 可用
                CameraSelector.Builder().requireLensFacing(cameraFacing).build()
            } else {
                // 否则 fallback 到第一个可用的相机
                Log.w("Camera", "Requested camera facing not available, falling back to the first available camera.")
                CameraSelector.Builder().addCameraFilter {
                    listOf(availableCameras.first()) // 使用第一个可用的相机
                }.build()
            }

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val frameAnalyzer = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            frameAnalyzer.setAnalyzer(Executors.newSingleThreadExecutor(), analyzer)

            cameraProvider.unbindAll()

            try {
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    frameAnalyzer
                )
            } catch (e: Exception) {
                Log.e("Camera", "Failed to bind camera: ${e.message}")
            }
        }, executor)

        if (childCount == 2) {
            removeView(this.previewView)
            removeView(this.boundingBoxOverlay)
        }

        this.previewView = previewView
        addView(this.previewView)

        val boundingBoxOverlayParams =
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        this.boundingBoxOverlay = BoundingBoxOverlay(context)
        this.boundingBoxOverlay.setWillNotDraw(false)
        this.boundingBoxOverlay.setZOrderOnTop(true)
        addView(this.boundingBoxOverlay, boundingBoxOverlayParams)
    }

//    @SuppressLint("RestrictedApi")
    private val analyzer =
        ImageAnalysis.Analyzer { image ->
            if (isProcessing) {
                image.close()
                return@Analyzer
            }
            isProcessing = true
//            val executionTime1 = measureTimeMillis {
            try{
                frameBitmap = image.toBitmap()
            }catch (e: Exception){
                Log.e("Error", "Failed to convert imageproxy to frameBitmap")
                image.close()
                return@Analyzer
            }
//            }

            // Transform android.net.Image to Bitmap
//            val executionTime2 = measureTimeMillis {
//                frameBitmap =
//                    Bitmap.createBitmap(
//                        image.image!!.width,
//                        image.image!!.height,
//                        Bitmap.Config.ARGB_8888
//                    )
//                frameBitmap.copyPixelsFromBuffer(image.planes[0].buffer)
//
//            }
//            println("執行時間：$executionTime1 毫秒, $executionTime2 毫秒")
//            "執行時間：3 毫秒, 14 毫秒"
//            "執行時間：1 毫秒, 6 毫秒"
            // Configure frameHeight and frameWidth for output2overlay transformation matrix
            // and apply it to `frameBitmap`
            if (!isImageTransformedInitialized) {
                imageTransform = Matrix()
                imageTransform.apply { postRotate(image.imageInfo.rotationDegrees.toFloat()) }
                isImageTransformedInitialized = true
            }
            frameBitmap =
                Bitmap.createBitmap(
                    frameBitmap,
                    0,
                    0,
                    frameBitmap.width,
                    frameBitmap.height,
                    imageTransform,
                    false
                )

            if (!isBoundingBoxTransformedInitialized) {

                val displayMetrics = context.resources.displayMetrics
                val screenWidth = displayMetrics.widthPixels.toFloat()
                val screenHeight = displayMetrics.heightPixels.toFloat()
                val topAppBarHeight = screenHeight - overlayHeight
                boundingBoxTransform = Matrix()
                boundingBoxTransform.apply {
                    setScale(
                        //overlayWidth / frameBitmap.width.toFloat(),
                        //overlayHeight / frameBitmap.height.toFloat()
                        screenWidth / frameBitmap.width.toFloat(),
                        screenHeight / frameBitmap.height.toFloat()
                    )
                    postTranslate(0f, -topAppBarHeight)
                    if (cameraFacing == CameraSelector.LENS_FACING_FRONT) {
                        // Mirror the bounding box coordinates
                        // for front-facing camera
                        postScale(
                            -1f,
                            1f,
                            // overlayWidth.toFloat() / 2.0f,
                            // overlayHeight.toFloat() / 2.0f
                            screenWidth.toFloat() / 2.0f,
                            screenHeight.toFloat() / 2.0f
                        )
                    }
                }
                isBoundingBoxTransformedInitialized = true
            }
            CoroutineScope(Dispatchers.Default).launch {
                val predictions = ArrayList<Prediction>()
                val (metrics, results) = viewModel.imageVectorUseCase.getNearestPersonName(frameBitmap, viewModel.preferencesManager.detectionConfidence.value)
                results.forEach {
                    (copppedface, person_id, boundingBox, spoofResult, cosineSimiliarity) ->
                    val box = boundingBox.toRectF()
                    var personId = person_id
                    var boxColor = Color.GRAY // Default color for unrecognized face

                    if (spoofResult != null && spoofResult.isSpoof) {
                        personId = 0
                        boxColor = Color.RED // Red for spoof
                    } else if (results.size > 1){
                        boxColor = Color.YELLOW // Yellow for multi faces
                    } else if (personId > 0) {
                        boxColor = Color.GREEN // Green for recognized face
                    }
                    boundingBoxTransform.mapRect(box)
                    predictions.add(Prediction(box, personId, true==spoofResult?.isSpoof, boxColor, cosineSimiliarity))
                }
                withContext(Dispatchers.Main) {
                    viewModel.faceDetectionMetricsState.value = metrics
                    this@FaceDetectionOverlay.predictions = predictions.toTypedArray()
                    boundingBoxOverlay.invalidate()
                    isProcessing = false
                }
            }
            image.close()
        }

    data class Prediction(var bbox: RectF, var person_id: Long, var isSpoof:Boolean, var boxColor: Int, var cosineSimiliarity: Float)

    inner class BoundingBoxOverlay(context: Context) :
        SurfaceView(context), SurfaceHolder.Callback {
        val em = 12
        private val boxPaint =
            Paint().apply {
                color = Color.BLUE
                style = Paint.Style.STROKE
                strokeWidth = 6f
            }
        private val textPaintBack =
            Paint().apply {
                strokeWidth = 2.0f
                textSize = em*3f
                color = Color.BLACK
                style = Paint.Style.STROKE
//                textAlign = Paint.Align.CENTER // 文本居中对齐
            }
        private val textPaintFront =
            Paint().apply {
                strokeWidth = 0.0f
                textSize = em*3f
                color = Color.WHITE
                style = Paint.Style.FILL
//                textAlign = Paint.Align.CENTER // 文本居中对齐
            }
        private val gaugeFront =
            Paint().apply {
                color = Color.BLUE
                style = Paint.Style.FILL
                strokeWidth = 6f
            }
        private val gaugeBack =
            Paint().apply {
                color = Color.GRAY
                style = Paint.Style.STROKE
                strokeWidth = 6f
            }
        override fun surfaceCreated(holder: SurfaceHolder) {}

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

        override fun surfaceDestroyed(holder: SurfaceHolder) {}

        override fun onDraw(canvas: Canvas) {
//            canvas.drawRect(0f, 0f, overlayWidth.toFloat(), overlayHeight.toFloat(), Paint().apply {
//                color = Color.RED
//                style = Paint.Style.STROKE
//                strokeWidth = 8f // Debug overlay
//            })
            predictions.forEach {
                // 繪製邊框
                boxPaint.color = it.boxColor
                canvas.drawRoundRect(it.bbox, 16f, 16f, boxPaint)

                val (id, confidence) = when {
                    it.isSpoof -> "照片" to ""
                    it.person_id > 0 -> "ID:${it.person_id}" to " (${(it.cosineSimiliarity*100).toInt()}%)"
                    else -> "路人" to ""
                }

                val tag = "$id$confidence"
                val shift = em*1.5f*tag.length/2
                canvas.drawText(tag, it.bbox.centerX() - shift, it.bbox.top  + 10 + em*3f, textPaintFront)
//                if (it.person_id > 0) {
                    draw_gauge(canvas, it)
//                }

            }
        }
        fun draw_gauge(canvas: Canvas, it:FaceDetectionOverlay.Prediction){
            val detectionDelay = viewModel.preferencesManager.detectionDelay.value
            if (viewModel.validFaceElapse.value > 0) {
                val percentage = if (viewModel.validFaceElapse.value > 0 && detectionDelay > 0) {
                    min(
                        100.0f,
                        viewModel.validFaceElapse.value.toFloat() * 100 / detectionDelay
                    ).toInt()
                } else {
                    0
                }

                // 計量表屬性
                val meterWidth = it.bbox.width() * 0.8f // 計量表寬度為邊框寬度的 80%
                val meterHeight = 40f // 計量表高度
                val meterLeft = it.bbox.centerX() - meterWidth / 2
                val meterTop = it.bbox.bottom + 20f
                val meterRight = meterLeft + meterWidth
                val meterBottom = meterTop + meterHeight

                // 創建漸變色填充 (從紅到黃到綠)
                val gradient = LinearGradient(
                    meterLeft, meterTop, meterRight, meterTop, // 漸變方向
                    intArrayOf(Color.RED, Color.YELLOW, Color.GREEN), // 顏色陣列
                    floatArrayOf(0f, 0.5f, 1f), // 漸變分布
                    Shader.TileMode.CLAMP
                )

                //計量表背景
                canvas.drawRoundRect(
                    meterLeft,
                    meterTop,
                    meterRight,
                    meterBottom,
                    10f,
                    10f,
                    gaugeBack
                )

                // 繪製進度部分（漸變色）
                val progressRight = meterLeft + (meterWidth * percentage / 100)
                gaugeFront.shader = gradient
                canvas.drawRoundRect(
                    meterLeft,
                    meterTop,
                    progressRight,
                    meterBottom,
                    10f,
                    10f,
                    gaugeFront
                )

                // 在計量表中間繪製文字
                val id = "ID:${it.person_id}"
                val shift = em * 1.5f * id.length / 2
                canvas.drawText(
                    id,
                    (meterLeft + meterRight) / 2 - shift,
                    meterTop + (meterHeight / 2) + em,
                    textPaintBack
                )
                canvas.drawText(
                    id,
                    (meterLeft + meterRight) / 2 - shift,
                    meterTop + (meterHeight / 2) + em,
                    textPaintFront
                )
            }

        }
    }
}
