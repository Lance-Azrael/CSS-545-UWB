package com.example.easyscreen

import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.MediaScannerConnection
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
//import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.io.FileOutputStream
import java.lang.Thread.sleep


class FloatingButtonService : Service() {

    private lateinit var sourceLanguage : String
    private lateinit var targetLanguage : String

    private lateinit var mediaProjection: MediaProjection
    private lateinit var virtualDisplay: VirtualDisplay
    private lateinit var imageReader: ImageReader
    private var resultData: Intent? = null
    private lateinit var windowManager: WindowManager
    private lateinit var floatingButton: View
    private lateinit var translateButton: ImageView
    private lateinit var translatedTextView: TextView
    private lateinit var back_button: Button
    private var cnt = 0

    private var xDelta = 0f
    private var yDelta = 0f

    private var windowX: Int = 0
    private var windowY: Int = 0

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        resultData = intent?.getParcelableExtra("media_projection_data")
        sourceLanguage = intent?.getStringExtra("source_language").toString()
        targetLanguage = intent?.getStringExtra("target_language").toString()
        println(sourceLanguage)

        if (resultData != null) {


            createNotification()
            val mediaProjectionManager =
                getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mediaProjectionManager.getMediaProjection(
                Activity.RESULT_OK,
                resultData!!
            )
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        floatingButton = LayoutInflater.from(this).inflate(R.layout.floating_button_layout, null)
        translateButton = floatingButton.findViewById(R.id.floatingButton)
        translatedTextView = floatingButton.findViewById(R.id.translatedText)
        back_button = floatingButton.findViewById(R.id.back_button)

        windowX = getScreenSizeInService(this).first
        windowY = getScreenSizeInService(this).second

        var move = false
        translateButton.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // 记录初始位置
                    xDelta = v.x - event.rawX
                    yDelta = v.y - event.rawY
                    move = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    // 更新按钮位置
                    v.animate()
                        .x(event.rawX + xDelta)
                        .y(event.rawY + yDelta)
                        .setDuration(0)
                        .start()
                    move = true
                    true
                }

                MotionEvent.ACTION_UP -> {
                    if (!move) {
                        // 点击事件
                        v.performClick()
                    }
                    true
                }

                else -> false
            }
        }

        translateButton.setOnClickListener {
//            takeScreenshot()
            cnt = 0
            Clip()
        }

        back_button.setOnClickListener {
            translatedTextView.visibility = View.INVISIBLE
            back_button.visibility = View.INVISIBLE
        }


        //        // 设置悬浮按钮的参数
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        // 设置悬浮按钮的位置
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 0

        // 添加悬浮按钮到窗口
        windowManager.addView(floatingButton, params)

//        if (intent?.action == "screenshot_action") {
//            takeScreenshot()
//        }
        return START_STICKY
    }


    fun getScreenSizeInService(service: Service): Pair<Int, Int> {
        val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 使用 WindowMetrics API (API Level 30 及以上)
            val windowMetrics = windowManager.currentWindowMetrics
            val insets = windowMetrics.windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.systemBars()
            )
            val width = windowMetrics.bounds.width() - insets.left - insets.right
            val height = windowMetrics.bounds.height() - insets.top - insets.bottom
            width to height
        } else {
            // 使用 DisplayMetrics (API Level 29 及以下)
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            val width = displayMetrics.widthPixels
            val height = displayMetrics.heightPixels
            width to height
        }
    }



    @RequiresApi(Build.VERSION_CODES.Q)
    private fun createNotification() {
        val channelId = "screenshot_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Screenshots",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        val screenshotIntent = Intent(this, FloatingButtonService::class.java).apply {
            action = "screenshot_action"
        }

        val pendingIntent =
            PendingIntent.getService(this, 0, screenshotIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Screenshot Service")
            .setContentText("Tap to take a screenshot")
            .setSmallIcon(R.drawable.ic_screenshot) // 使用适当的图标
            .addAction(R.drawable.ic_screenshot, "Screenshot", pendingIntent)
            .build()

        startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
    }

    private fun takeScreenshot() {
        // 设置图像读取器
        imageReader = ImageReader.newInstance(1080, 1920, PixelFormat.RGBA_8888, 1)

        virtualDisplay = mediaProjection.createVirtualDisplay(
            "Screenshot",
            1080, 1920, resources.displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface, null, null
        )

        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            // 处理图像保存或其他操作
            saveImage(image)
            image.close()
        }, null)

        // 提示：在适当的时候释放资源
//        imageReader.close()
//        virtualDisplay.release()
//        mediaProjection.stop()

    }

    private fun Clip() {
        // 创建截图覆盖层
        println("takeScreenshot")
        val overlay = LayoutInflater.from(this).inflate(R.layout.screenshot_overlay, null)
        val selectionView = overlay.findViewById<View>(R.id.selection_view)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        // 显示覆盖层
        windowManager.addView(overlay, params)

        // 处理用户选择区域
        selectionView.setOnTouchListener(object : View.OnTouchListener {
            private var startX = 0f
            private var startY = 0f
            private var endX = 0f
            private var endY = 0f
            private var selectionRect: View? = null

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = event.x
                        startY = event.y
                        // 创建选择矩形视图
                        selectionRect = View(this@FloatingButtonService).apply {
                            layoutParams = ViewGroup.LayoutParams(0, 0)
                            setBackgroundColor(Color.TRANSPARENT)
                        }
                        windowManager.addView(selectionRect, params)
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        endX = event.x
                        endY = event.y
                        updateSelectionRect(startX, startY, endX, endY)
                        return true
                    }

                    MotionEvent.ACTION_UP -> {
                        // 在这里处理截图逻辑
//                        captureScreenshot(startX, startY, endX, endY)
                        cnt = 0
                        windowManager.removeView(selectionRect) // 移除选择矩形
                        windowManager.removeView(overlay) // 移除覆盖层
                        captureScreenshot(startX, startY, endX, endY)
//                        translateButton.visibility = View.INVISIBLE
//                        overlay.visibility = View.INVISIBLE


//                        stopSelf()
                        return true
                    }
                }
                return false
            }


            private fun translateCroppedBitmap(bitmap: Bitmap) {
                cnt++
                val imagesDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                println("enter translateCroppedBitmap")
                val file = File(
                    imagesDir, "screenshot.png"
                )
                val screenshotFile = File(imagesDir, "screenshot.png")
                // 创建文本识别器
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)


                val inputImage = InputImage.fromFilePath(
                    this@FloatingButtonService,
                    Uri.fromFile(screenshotFile)
                )

                // 进行文本识别
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        // 提取识别的文本
                        val recognizedText = visionText.text
                        if (recognizedText.isNotEmpty()) {
                            // 调用翻译函数
                            translateText(recognizedText)
//                            Log.d("recognizedText", recognizedText)
                        } else {
                            println("未识别到任何文本")
//                            Toast.makeText(this@FloatingButtonService, "未识别到任何文本", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        e.printStackTrace()
                        println("文本识别失败")
//                        Toast.makeText(this@FloatingButtonService, "文本识别失败", Toast.LENGTH_SHORT).show()
                    }
            }

            private fun languageSelected(language: String): String {
                return when (language) {
                    "Chinese" -> TranslateLanguage.CHINESE
                    "Spanish" -> TranslateLanguage.SPANISH
                    "French" -> TranslateLanguage.FRENCH
                    else -> TranslateLanguage.ENGLISH
                }
            }

            private fun translateText(text: String) {
                println("enter translateText")
                val options = TranslatorOptions.Builder()
                    .setSourceLanguage(languageSelected(sourceLanguage))
                    .setTargetLanguage(languageSelected(targetLanguage))
                    .build()
                val translator = Translation.getClient(options)

                var conditions = DownloadConditions.Builder()
                    .requireWifi()
                    .build()
                translator.downloadModelIfNeeded(conditions)
                    .addOnSuccessListener {
                        println("Download success")
//                        Toast.makeText(this@FloatingButtonService, "Download success", Toast.LENGTH_LONG).show()
                    }
                    .addOnFailureListener { exception ->
                        println("Download failed")
//                        Toast.makeText(this@FloatingButtonService, "Download failed", Toast.LENGTH_LONG).show()
                    }

                translator.translate(text)
                    .addOnSuccessListener { translatedText ->
                        // 显示翻译结果
                        println("Translate result: $translatedText")
                        translatedTextView.visibility = View.VISIBLE
                        translatedTextView.text = "$translatedText"
                        back_button.visibility = View.VISIBLE
                    }
                    .addOnFailureListener { e ->
                        e.printStackTrace()
                        println("翻译失败")
//                        Toast.makeText(this@FloatingButtonService, "翻译失败", Toast.LENGTH_SHORT).show()
                    }
            }

            private fun updateSelectionRect(
                startX: Float,
                startY: Float,
                endX: Float,
                endY: Float
            ) {
                val left = minOf(startX, endX).toInt()
                val top = minOf(startY, endY).toInt()
                val right = maxOf(startX, endX).toInt()
                val bottom = maxOf(startY, endY).toInt()

                selectionRect?.layout(left, top, right, bottom)
                selectionRect?.setBackgroundColor(Color.argb(128, 255, 255, 255)) // 半透明白色
            }


            private fun captureScreenshot(startX: Float, startY: Float, endX: Float, endY: Float) {
                println("enter captureScreenshot")
                imageReader = ImageReader.newInstance(windowX, windowY, PixelFormat.RGBA_8888, 1)
                println("imageReader created")
                virtualDisplay = mediaProjection.createVirtualDisplay(
                    "Screenshot",
                    1080, 1920, resources.displayMetrics.densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.surface, null, null
                )
                println("virtualDisplay created")

                imageReader.setOnImageAvailableListener({ reader ->
                    if (cnt == 0) {
                        println("enter setOnImageAvailableListener")
                        sleep(1000)
                        val image = reader.acquireNextImage()
                        if (image != null) {
                            val buffer = image.planes[0].buffer
                            val width = image.width
                            val height = image.height
                            val pixels = IntArray(width * height)

                            // 读取图像数据到数组中
                            buffer.rewind()
                            buffer.asIntBuffer().get(pixels)

                            // 计算选择区域的边界
                            val left = minOf(startX, endX).toInt()
                            val top = minOf(startY, endY).toInt()
                            val right = maxOf(startX, endX).toInt()
                            val bottom = maxOf(startY, endY).toInt()

                            // 剪切图像
                            val croppedBitmap =
                                Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
                            val finalBitmap = Bitmap.createBitmap(
                                croppedBitmap,
                                left,
                                top,
                                right - left,
                                bottom - top
                            )

                            // 保存图像
//                        translateCroppedBitmap(finalBitmap)
                            saveImage(finalBitmap)
                            translateCroppedBitmap(finalBitmap)
                            croppedBitmap.recycle()
                            finalBitmap.recycle()
                        }
                        image.close()
                        imageReader.close()
                        virtualDisplay.release()
                    }


//                    imageReader.close()
//                    virtualDisplay.release()


                }, null)

                // 提示：在适当的时候释放资源
//                println("release resource")

//                mediaProjection.stop()
            }


        })
//        stopSelf()
    }

    private fun saveImage(bitmap: Bitmap) {
        println("enter saveImage")
        cnt++
        val imagesDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
//        println(imagesDir)
        val file = File(
            imagesDir, "screenshot.png"
        )
        val screenshotFile = File(imagesDir, "screenshot.png")
        FileOutputStream(file).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
        }
        MediaScannerConnection.scanFile(this, arrayOf(screenshotFile.path), null, null)
        println("capture saved")
//        Toast.makeText(this, "Screenshot saved", Toast.LENGTH_SHORT).show()
//        stopSelf()
    }


    private fun saveImage(image: Image) {
        val bitmap = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(image.planes[0].buffer)

        val imagesDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val screenshotFile = File(imagesDir, "screenshot.png")

        val fos = FileOutputStream(screenshotFile)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        fos.close()

        MediaScannerConnection.scanFile(this, arrayOf(screenshotFile.path), null, null)

//        Toast.makeText(this, "Screenshot saved", Toast.LENGTH_SHORT).show()
//        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清理资源

//        windowManager.removeView(translateButton)
//        windowManager.removeView(translatedTextView)
        windowManager.removeView(floatingButton)
        mediaProjection.stop()

    }
}
