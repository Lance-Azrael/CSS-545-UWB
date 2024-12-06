package com.example.easyscreen

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.text.method.ScrollingMovementMethod
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.lang.Thread.sleep


class FloatingButtonService : Service() {

    private lateinit var sourceLanguage: String
    private lateinit var targetLanguage: String

    private lateinit var mediaProjection: MediaProjection
    private lateinit var virtualDisplay: VirtualDisplay
    private lateinit var imageReader: ImageReader
    private var resultData: Intent? = null
    private lateinit var windowManager: WindowManager
    private lateinit var floatingButton: View
    private lateinit var translateButton: ImageView
    private lateinit var translatedTextView: TextView
    private lateinit var back_button: ImageButton

    private lateinit var remoteModelManager: RemoteModelManager

    private var mediaProjectionManager: MediaProjectionManager? = null
    private var cnt = 0

    private var startX = 0
    private var startY = 0

    private var windowX: Int = 0
    private var windowY: Int = 0

    private var lastX = 0f
    private var lastY = 0f
    private var isDragging = false

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        resultData = intent?.getParcelableExtra("media_projection_data")
        sourceLanguage = intent?.getStringExtra("source_language").toString()
        targetLanguage = intent?.getStringExtra("target_language").toString()
        remoteModelManager = RemoteModelManager.getInstance()
        println(sourceLanguage)
        println(targetLanguage)


        // Media Projection
        if (resultData != null) {
            createNotification()
            mediaProjectionManager =
                getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

            mediaProjection = mediaProjectionManager!!.getMediaProjection(
                Activity.RESULT_OK,
                resultData!!
            )

        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        floatingButton = LayoutInflater.from(this).inflate(R.layout.floating_button_layout, null)
        translateButton = floatingButton.findViewById(R.id.floatingButton)
        translatedTextView = floatingButton.findViewById<TextView?>(R.id.translatedText).apply {
            movementMethod = ScrollingMovementMethod()
            setHorizontallyScrolling(false)
        }
        back_button = floatingButton.findViewById(R.id.back_button)

        windowX = getScreenSizeInService(this).first
        windowY = getScreenSizeInService(this).second


        // floating button click event
        var move = false
        translateButton.setOnTouchListener { v, event ->
            when (event.action) {

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
            Toast.makeText(this, "Tap and drag to select text", Toast.LENGTH_SHORT).show()
            cnt = 0
            Clip()
        }

        val displayMetrics = resources.displayMetrics
        val density = displayMetrics.density

        val widthInPx = (60 * density).toInt()  // 100dp convert to pixels
        val heightInPx = (60 * density).toInt()  // 100dp convert to pixels


        // back button click event
        back_button.setOnClickListener {
            translatedTextView.visibility = View.INVISIBLE
            back_button.visibility = View.INVISIBLE
            var params = floatingButton.layoutParams
            params.width = widthInPx
            params.height = heightInPx
            windowManager.updateViewLayout(floatingButton, params)

        }


        // floating button position
        val params = WindowManager.LayoutParams(
            widthInPx,
            heightInPx,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 0

        floatingButton.setOnTouchListener { v, event ->
            val params = v.layoutParams as WindowManager.LayoutParams
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // start position
                    startX = event.rawX.toInt()
                    startY = event.rawY.toInt()
                }

                MotionEvent.ACTION_MOVE -> {
                    // new position
                    val dx = event.rawX.toInt() - startX
                    val dy = event.rawY.toInt() - startY
                    params.x += dx
                    params.y += dy
                    windowManager.updateViewLayout(v, params)
                    startX = event.rawX.toInt()
                    startY = event.rawY.toInt()
                }
            }
            true
        }

        // text box position
        translatedTextView.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    // start position
                    lastX = event.rawX
                    lastY = event.rawY
                    isDragging = false
                }

                MotionEvent.ACTION_MOVE -> {
                    if (event.pointerCount == 1) {
                        // single finger operation (scroll text)
                        false
                    } else if (event.pointerCount == 2) {
                        // two finger operation (move text box)
                        val deltaX = event.rawX - lastX
                        val deltaY = event.rawY - lastY
                        params.x += deltaX.toInt()
                        params.y += deltaY.toInt()
                        windowManager.updateViewLayout(floatingButton, params)
                        lastX = event.rawX
                        lastY = event.rawY
                        true
                    } else {
                        false
                    }
                }

                MotionEvent.ACTION_UP -> {
                    if (isDragging) {
                        isDragging = false
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }

        windowManager.addView(floatingButton, params)

        return START_STICKY
    }


    // get screen size
    fun getScreenSizeInService(service: Service): Pair<Int, Int> {
        val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // WindowMetrics API (API Level 30 or higher)
            val windowMetrics = windowManager.currentWindowMetrics
            val insets = windowMetrics.windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.systemBars()
            )
            val width = windowMetrics.bounds.width() - insets.left - insets.right
            val height = windowMetrics.bounds.height() - insets.top - insets.bottom
            width to height
        } else {
            // DisplayMetrics (API Level 29 or lower)
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            val width = displayMetrics.widthPixels
            val height = displayMetrics.heightPixels
            width to height
        }
    }


    // create notification
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
            .setSmallIcon(R.drawable.ic_screenshot)
            .addAction(R.drawable.ic_screenshot, "Screenshot", pendingIntent)
            .build()

        startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
    }



    private fun Clip() {
        println("takeScreenshot")

        // overlay
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

        windowManager.addView(overlay, params)

        // selection view
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
                        // create selection rectangle
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

                    // after selection
                    MotionEvent.ACTION_UP -> {
                        cnt = 0
                        windowManager.removeView(selectionRect)
                        windowManager.removeView(overlay)
                        captureScreenshot(startX, startY, endX, endY)
                        return true
                    }
                }
                return false
            }


            private fun translateCroppedBitmap(bitmap: Bitmap) {
                val outputBitmap =
                    Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(outputBitmap)
                canvas.drawBitmap(bitmap, 0f, 0f, null)

                cnt++
//                val imagesDir =
//                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
//                val file = File(imagesDir, "screenshot.png")
//                val screenshotFile = File(imagesDir, "screenshot.png")

                // recognizer
                var recognizer: com.google.mlkit.vision.text.TextRecognizer
                if (sourceLanguage == "Chinese") {
                    recognizer =
                        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
                } else if (sourceLanguage == "English") {
                    recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                } else {
                    recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                }


//                val inputImage1 = InputImage.fromFilePath(this@FloatingButtonService, Uri.fromFile(screenshotFile))
                val inputImage = InputImage.fromBitmap(outputBitmap, 0)

                // recognize process
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val recognizedText = visionText.text
                        if (recognizedText.isNotEmpty()) {
                            val sentences = splitSentencesWithNewlineAndCase(recognizedText)
                            val cleanedText = sentences.joinToString("\n")
                            translateText(cleanedText)
                        } else {
                            translateText("Text not recognized")
                            println("no text recognized")}
                    }
                    .addOnFailureListener { e ->
                        e.printStackTrace()
                        println("text recognition failed")
                    }
            }

            // language to model
            private fun languageSelected(language: String): String {
                return when (language) {
                    "Afrikaans" -> TranslateLanguage.AFRIKAANS
                    "Albanian" -> TranslateLanguage.ALBANIAN
                    "Arabic" -> TranslateLanguage.ARABIC
                    "Belarusian" -> TranslateLanguage.BELARUSIAN
                    "Bengali" -> TranslateLanguage.BENGALI
                    "Bulgarian" -> TranslateLanguage.BULGARIAN
                    "Catalan" -> TranslateLanguage.CATALAN
                    "Chinese" -> TranslateLanguage.CHINESE
                    "Croatian" -> TranslateLanguage.CROATIAN
                    "Czech" -> TranslateLanguage.CZECH
                    "Danish" -> TranslateLanguage.DANISH
                    "Dutch" -> TranslateLanguage.DUTCH
                    "English" -> TranslateLanguage.ENGLISH
                    "Esperanto" -> TranslateLanguage.ESPERANTO
                    "Estonian" -> TranslateLanguage.ESTONIAN
                    "Finnish" -> TranslateLanguage.FINNISH
                    "French" -> TranslateLanguage.FRENCH
                    "Galician" -> TranslateLanguage.GALICIAN
                    "Georgian" -> TranslateLanguage.GEORGIAN
                    "German" -> TranslateLanguage.GERMAN
                    "Greek" -> TranslateLanguage.GREEK
                    "Gujarati" -> TranslateLanguage.GUJARATI
                    "Haitian Creole" -> TranslateLanguage.HAITIAN_CREOLE
                    "Hebrew" -> TranslateLanguage.HEBREW
                    "Hindi" -> TranslateLanguage.HINDI
                    "Hungarian" -> TranslateLanguage.HUNGARIAN
                    "Icelandic" -> TranslateLanguage.ICELANDIC
                    "Indonesian" -> TranslateLanguage.INDONESIAN
                    "Irish" -> TranslateLanguage.IRISH
                    "Italian" -> TranslateLanguage.ITALIAN
                    "Japanese" -> TranslateLanguage.JAPANESE
                    "Kannada" -> TranslateLanguage.KANNADA
                    "Korean" -> TranslateLanguage.KOREAN
                    "Lithuanian" -> TranslateLanguage.LITHUANIAN
                    "Latvian" -> TranslateLanguage.LATVIAN
                    "Macedonian" -> TranslateLanguage.MACEDONIAN
                    "Marathi" -> TranslateLanguage.MARATHI
                    "Malay" -> TranslateLanguage.MALAY
                    "Maltese" -> TranslateLanguage.MALTESE
                    "Norwegian" -> TranslateLanguage.NORWEGIAN
                    "Persian" -> TranslateLanguage.PERSIAN
                    "Polish" -> TranslateLanguage.POLISH
                    "Portuguese" -> TranslateLanguage.PORTUGUESE
                    "Romanian" -> TranslateLanguage.ROMANIAN
                    "Russian" -> TranslateLanguage.RUSSIAN
                    "Slovak" -> TranslateLanguage.SLOVAK
                    "Slovenian" -> TranslateLanguage.SLOVENIAN
                    "Spanish" -> TranslateLanguage.SPANISH
                    "Swahili" -> TranslateLanguage.SWAHILI
                    "Swedish" -> TranslateLanguage.SWEDISH
                    "Tagalog" -> TranslateLanguage.TAGALOG
                    "Tamil" -> TranslateLanguage.TAMIL
                    "Telugu" -> TranslateLanguage.TELUGU
                    "Thai" -> TranslateLanguage.THAI
                    "Turkish" -> TranslateLanguage.TURKISH
                    "Ukrainian" -> TranslateLanguage.UKRAINIAN
                    "Urdu" -> TranslateLanguage.URDU
                    "Vietnamese" -> TranslateLanguage.VIETNAMESE
                    "Welsh" -> TranslateLanguage.WELSH
                    else -> TranslateLanguage.ENGLISH
                }
            }

            // translate text
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
//                val remoteModel = TranslateRemoteModel.Builder(languageSelected(targetLanguage)).build()
//                remoteModelManager.download(remoteModel, conditions)
                translator.downloadModelIfNeeded(conditions)
                    .addOnSuccessListener {
                        println("Download success")
                        translator.translate(text)
                            .addOnSuccessListener { translatedText ->
                                println("Translate result: $translatedText")
                                translatedTextView.visibility = View.VISIBLE
                                translatedTextView.text = "$translatedText"
                                back_button.visibility = View.VISIBLE
                                //update the width and height of floatting button
                                var params = floatingButton.layoutParams
                                params.width = WindowManager.LayoutParams.WRAP_CONTENT
                                params.height = WindowManager.LayoutParams.WRAP_CONTENT
                                windowManager.updateViewLayout(floatingButton, params)
                                Toast.makeText(
                                    this@FloatingButtonService,
                                    "One finger to scroll\nTwo fingers to move",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener { e ->
                                e.printStackTrace()
                                println("translate failed")
//                        Toast.makeText(this@FloatingButtonService, "翻译失败", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener { exception ->
                        println("Download failed")
//                        Toast.makeText(this@FloatingButtonService, "Download failed", Toast.LENGTH_SHORT).show()
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
                val displayMetrics = resources.displayMetrics
                val screenWidth = displayMetrics.widthPixels
                val screenHeight = displayMetrics.heightPixels
//                val screenWidth = 1080
//                val screenHeight = 2400
//                Toast.makeText(this@FloatingButtonService, screenWidth.toString(), Toast.LENGTH_SHORT).show()
//                Toast.makeText(this@FloatingButtonService, screenHeight.toString(), Toast.LENGTH_SHORT).show()
                imageReader =
                    ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 1)
                println("imageReader created")

                mediaProjection = mediaProjectionManager!!.getMediaProjection(
                    Activity.RESULT_OK,
                    resultData!!
                )
                virtualDisplay = mediaProjection.createVirtualDisplay(
                    "Screenshot",
                    screenWidth, screenHeight, resources.displayMetrics.densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.surface, null, null
                )
                println("virtualDisplay created")

                imageReader.setOnImageAvailableListener({ reader ->
                    if (cnt == 0) {
                        println("enter setOnImageAvailableListener")
                        sleep(600)
                        val image = reader.acquireNextImage()
                        if (image != null) {
                            val width = image.width
                            val height = image.height
                            val planes = image.planes
                            val buffer = planes[0].buffer
                            val pixelStride = planes[0].pixelStride
                            val rowStride = planes[0].rowStride
                            val rowPadding = rowStride - pixelStride * width
                            val bitmap = Bitmap.createBitmap(
                                width + rowPadding / pixelStride,
                                height,
                                Bitmap.Config.ARGB_8888
                            )
                            bitmap.copyPixelsFromBuffer(buffer);
//                            val buffer = image.planes[0].buffer
//                            val width = image.width
//                            val height = image.height
//                            val pixels = IntArray(width * height)

//                            // read image into buffer
//                            buffer.rewind()
//                            buffer.asIntBuffer().get(pixels)

                            // culculate the clip area
                            val left = minOf(startX, endX).toInt()
                            val top = minOf(startY, endY).toInt()
                            val right = maxOf(startX, endX).toInt()
                            val bottom = maxOf(startY, endY).toInt()

//                            val croppedBitmap =
//                                Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
                            val finalBitmap = Bitmap.createBitmap(
                                bitmap,
                                left,
                                top,
                                right - left,
                                bottom - top
                            )

                            // save image
//                        translateCroppedBitmap(finalBitmap)
//                            saveImage(finalBitmap)

                            translateCroppedBitmap(finalBitmap)
//                            translateCroppedBitmap(bitmap)
//                            croppedBitmap.recycle()
                            bitmap.recycle()
                            finalBitmap.recycle()
                        }
                        image.close()
                        imageReader.close()
                        virtualDisplay.release()
                    }

//                    imageReader.close()
//                    virtualDisplay.release()

                }, null)
            }
        })
    }

//    private fun saveImage(bitmap: Bitmap) {
//        println("enter saveImage")
//        cnt++
//        val imagesDir =
//            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
////        println(imagesDir)
//        val file = File(
//            imagesDir, "screenshot.png"
//        )
//        val screenshotFile = File(imagesDir, "screenshot.png")
//        FileOutputStream(file).use { outputStream ->
//            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
//            outputStream.flush()
//        }
//        MediaScannerConnection.scanFile(this, arrayOf(screenshotFile.path), null, null)
//        println("capture saved")
////        Toast.makeText(this, "Screenshot saved", Toast.LENGTH_SHORT).show()
////        stopSelf()
//    }


//    private fun saveImage(image: Image) {
//        val bitmap = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
//        bitmap.copyPixelsFromBuffer(image.planes[0].buffer)
//
//        val imagesDir =
//            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
//        val screenshotFile = File(imagesDir, "screenshot.png")
//
//        val fos = FileOutputStream(screenshotFile)
//        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
//        fos.close()
//
//        MediaScannerConnection.scanFile(this, arrayOf(screenshotFile.path), null, null)
//
////        Toast.makeText(this, "Screenshot saved", Toast.LENGTH_SHORT).show()
////        stopSelf()
//    }


    // optimize english text
    fun splitSentencesWithNewlineAndCase(text: String): List<String> {
        val sentences = mutableListOf<String>()
        val paragraphs = text.split("\n")

        var currentSentence = StringBuilder()

        paragraphs.forEach { paragraph ->
            if (paragraph.isNotBlank()) {
                val trimmedParagraph = paragraph.trim()
                if (trimmedParagraph.first().isUpperCase() && currentSentence.isNotEmpty()) {
                    val sentenceToAdd = ensureSentenceEndsWithPeriod(currentSentence.toString())
                    sentences.add(sentenceToAdd)
                    currentSentence = StringBuilder(trimmedParagraph)
                } else {
                    if (currentSentence.isNotEmpty()) currentSentence.append(" ")
                    currentSentence.append(trimmedParagraph)
                }
            }
        }

        if (currentSentence.isNotEmpty()) {
            val sentenceToAdd = ensureSentenceEndsWithPeriod(currentSentence.toString())
            sentences.add(sentenceToAdd)
        }

        return sentences
    }

    fun ensureSentenceEndsWithPeriod(sentence: String): String {
        return if (sentence.lastOrNull() in listOf('.', '!', '?')) {
            sentence
        } else {
            "$sentence."
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
//        windowManager.removeView(translateButton)
//        windowManager.removeView(translatedTextView)
        windowManager.removeView(floatingButton)
        mediaProjection.stop()
    }
}
