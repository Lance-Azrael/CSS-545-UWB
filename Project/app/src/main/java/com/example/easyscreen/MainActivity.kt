package com.example.easyscreen

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.IOException


class MainActivity : ComponentActivity() {

    private lateinit var sourceLanguageSpinner: Spinner
    private lateinit var targetLanguageSpinner: Spinner
    private var selectedSourceLanguage: String = "Select Language"
    private var selectedTargetLanguage: String = "Select Language"
    private lateinit var userButton: Button

    private lateinit var startButton: Button
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var screenCaptureLauncher: ActivityResultLauncher<Intent>
    private var resultData: Intent? = null
    private var isServiceStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // language spinner
        sourceLanguageSpinner = findViewById(R.id.source_language_spinner)
        val sourceLanguages = arrayOf("English", "Chinese", "Spanish", "French")
        val sourceAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sourceLanguages)
        sourceLanguageSpinner.adapter = sourceAdapter

        targetLanguageSpinner = findViewById(R.id.target_language_spinner)
        val targetLanguages = arrayOf("English", "Chinese", "Spanish", "French")
        val targetAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, targetLanguages)
        targetLanguageSpinner.adapter = targetAdapter

        // language preference
        val sharedPrefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        selectedSourceLanguage = sharedPrefs.getString("sourceLanguage", "English") ?: "English"
        selectedTargetLanguage = sharedPrefs.getString("targetLanguage", "English") ?: "English"
        sourceLanguageSpinner.setSelection(sourceLanguages.indexOf(selectedSourceLanguage))
        targetLanguageSpinner.setSelection(targetLanguages.indexOf(selectedTargetLanguage))

        // listener for selecting language
        sourceLanguageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val newLanguage = sourceLanguages[position]
                if (newLanguage != selectedSourceLanguage) {
                    selectedSourceLanguage = newLanguage
                    sharedPrefs.edit().putString("sourceLanguage", selectedSourceLanguage).apply()
                    Toast.makeText(this@MainActivity, "重启翻译功能时生效", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        targetLanguageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val newLanguage = targetLanguages[position]
                if (newLanguage != selectedTargetLanguage) {
                    selectedTargetLanguage = newLanguage
                    sharedPrefs.edit().putString("targetLanguage", selectedTargetLanguage).apply()
                    Toast.makeText(this@MainActivity, "重启翻译功能时生效", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 用户界面跳转按钮
        userButton = findViewById(R.id.user_button)
        userButton.setOnClickListener {
            val intent = Intent(this, UserActivity::class.java)
            startActivity(intent)
        }


        startButton = findViewById(R.id.startButton)

        mediaProjectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        screenCaptureLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                resultData = result.data
                // 启动 Service 并传递权限数据
                //startMyService(resultData)
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent =
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            val OVERLAY_PERMISSION_REQUEST_CODE = 1234
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
        }

        startButton.setOnClickListener {
            if (!isServiceStarted) {
                startMyService(resultData)
                startButton.text = "Stop"
                startButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_stop, 0, 0, 0)
            } else {
                stopMyService()
                startButton.text = "Start"
                startButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_start, 0, 0, 0)
            }
        }

        startScreenCapture()

    }

    private fun startScreenCapture() {
        val intent = mediaProjectionManager.createScreenCaptureIntent()
        screenCaptureLauncher.launch(intent)
    }

    private fun startMyService(resultData: Intent?) {
        val serviceIntent = Intent(this, FloatingButtonService::class.java).apply {
            putExtra("media_projection_data", resultData)
            putExtra("source_language", selectedSourceLanguage)
            putExtra("target_language", selectedTargetLanguage)
        }
        startForegroundService(serviceIntent)
        isServiceStarted = true
    }

    private fun stopMyService() {
        val serviceIntent = Intent(this, FloatingButtonService::class.java)
        stopService(serviceIntent)
        isServiceStarted = false
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMyService()
    }
}

