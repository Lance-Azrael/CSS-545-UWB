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
import com.google.firebase.FirebaseApp
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
        FirebaseApp.initializeApp(this)

        // language spinner
//        sourceLanguageSpinner = findViewById(R.id.source_language_spinner)
//        val sourceLanguages = arrayOf("English", "Chinese", "Spanish", "French")
//        val sourceAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sourceLanguages)
//        sourceLanguageSpinner.adapter = sourceAdapter
//
//        targetLanguageSpinner = findViewById(R.id.target_language_spinner)
//        val targetLanguages = arrayOf("English", "Chinese", "Spanish", "French")
//        val targetAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, targetLanguages)
//        targetLanguageSpinner.adapter = targetAdapter

        // language spinner
        sourceLanguageSpinner = findViewById(R.id.source_language_spinner)
//        val sourceLanguages = arrayOf(
//            "Afrikaans", "Albanian", "Arabic", "Belarusian", "Bengali", "Bulgarian", "Catalan",
//            "Chinese", "Croatian", "Czech", "Danish", "Dutch", "English", "Esperanto", "Estonian",
//            "Finnish", "French", "Galician", "Georgian", "German", "Greek", "Gujarati",
//            "Haitian Creole", "Hebrew", "Hindi", "Hungarian", "Icelandic", "Indonesian",
//            "Irish", "Italian", "Japanese", "Kannada", "Korean", "Lithuanian", "Latvian",
//            "Macedonian", "Marathi", "Malay", "Maltese", "Norwegian", "Persian", "Polish",
//            "Portuguese", "Romanian", "Russian", "Slovak", "Slovenian", "Spanish", "Swahili",
//            "Swedish", "Tagalog", "Tamil", "Telugu", "Thai", "Turkish", "Ukrainian", "Urdu",
//            "Vietnamese", "Welsh"
//        )
        val sourceLanguages = arrayOf(
            "Afrikaans (Afrikaans)", "Catalan (Català)", "Czech (Čeština)",
            "Danish (Dansk)", "Dutch (Nederlands)", "English (English)", "Estonian (Eesti)",
            "Finnish (Suomi)", "French (Français)", "German (Deutsch)", "Hungarian (Magyar)",
            "Indonesian (Bahasa Indonesia)", "Italian (Italiano)", "Malay (Bahasa Melayu)",
            "Norwegian (Norsk)", "Polish (Polski)", "Portuguese (Português)", "Romanian (Română)",
            "Slovak (Slovenčina)", "Spanish (Español)", "Swedish (Svenska)", "Turkish (Türkçe)",
            "Vietnamese (Tiếng Việt)"
        )
        val sourceAdapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sourceLanguages)
        sourceLanguageSpinner.adapter = sourceAdapter

        targetLanguageSpinner = findViewById(R.id.target_language_spinner)
        val targetLanguages = arrayOf(
            "Afrikaans (Afrikaans)",
            "Albanian (Shqip)",
            "Arabic (العربية)",
            "Belarusian (Беларуская)",
            "Bengali (বাংলা)",
            "Bulgarian (Български)",
            "Catalan (Català)",
            "Chinese (中文)",
            "Croatian (Hrvatski)",
            "Czech (Čeština)",
            "Danish (Dansk)",
            "Dutch (Nederlands)",
            "English (English)",
            "Esperanto (Esperanto)",
            "Estonian (Eesti)",
            "Finnish (Suomi)",
            "French (Français)",
            "Galician (Galego)",
            "Georgian (ქართული)",
            "German (Deutsch)",
            "Greek (Ελληνικά)",
            "Gujarati (ગુજરાતી)",
            "Haitian Creole (Kreyòl Ayisyen)",
            "Hebrew (עברית)",
            "Hindi (हिन्दी)",
            "Hungarian (Magyar)",
            "Icelandic (Íslenska)",
            "Indonesian (Bahasa Indonesia)",
            "Irish (Gaeilge)",
            "Italian (Italiano)",
            "Japanese (日本語)",
            "Kannada (ಕನ್ನಡ)",
            "Korean (한국어)",
            "Lithuanian (Lietuvių)",
            "Latvian (Latviešu)",
            "Macedonian (Македонски)",
            "Marathi (मराठी)",
            "Malay (Bahasa Melayu)",
            "Maltese (Malti)",
            "Norwegian (Norsk)",
            "Persian (فارسی)",
            "Polish (Polski)",
            "Portuguese (Português)",
            "Romanian (Română)",
            "Russian (Русский)",
            "Slovak (Slovenčina)",
            "Slovenian (Slovenščina)",
            "Spanish (Español)",
            "Swahili (Kiswahili)",
            "Swedish (Svenska)",
            "Tagalog (Tagalog)",
            "Tamil (தமிழ்)",
            "Telugu (తెలుగు)",
            "Thai (ไทย)",
            "Turkish (Türkçe)",
            "Ukrainian (Українська)",
            "Urdu (اردو)",
            "Vietnamese (Tiếng Việt)",
            "Welsh (Cymraeg)"
        )
        val targetAdapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, targetLanguages)
        targetLanguageSpinner.adapter = targetAdapter



        // language preference
        val sharedPrefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        selectedSourceLanguage =
            sharedPrefs.getString("sourceLanguage", "English (English)") ?: "English (English)"
        selectedTargetLanguage =
            sharedPrefs.getString("targetLanguage", "English (English)") ?: "English (English)"
        sourceLanguageSpinner.setSelection(sourceLanguages.indexOf(selectedSourceLanguage))
        targetLanguageSpinner.setSelection(targetLanguages.indexOf(selectedTargetLanguage))

        // listener for selecting language
        sourceLanguageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val newLanguage = sourceLanguages[position]
                if (newLanguage != selectedSourceLanguage) {
                    selectedSourceLanguage = newLanguage
                    sharedPrefs.edit().putString("sourceLanguage", selectedSourceLanguage).apply()
                    Toast.makeText(
                        this@MainActivity,
                        "Restart service to change language",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    Toast.makeText(
                        this@MainActivity,
                        "First translation takes more time",
                        Toast.LENGTH_SHORT
                    )
                        .show()
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
                    Toast.makeText(
                        this@MainActivity,
                        "Restart service to change language",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    Toast.makeText(
                        this@MainActivity,
                        "First translation takes more time",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // user activity
        userButton = findViewById(R.id.user_button)
        userButton.setOnClickListener {
            val intent = Intent(this, UserActivity::class.java)
            startActivity(intent)
        }

        // start button
        startButton = findViewById(R.id.startButton)

        // screen capture service
        mediaProjectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        screenCaptureLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                resultData = result.data
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
//                stopMyService()
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

    fun extractLanguageName(language: String): String {
        return language.split(" (")[0]
    }

    private fun startScreenCapture() {
        val intent = mediaProjectionManager.createScreenCaptureIntent()
        screenCaptureLauncher.launch(intent)
    }

    private fun startMyService(resultData: Intent?) {
        val serviceIntent = Intent(this, FloatingButtonService::class.java).apply {
            putExtra("media_projection_data", resultData)
            putExtra("source_language", extractLanguageName(selectedSourceLanguage))
            putExtra("target_language", extractLanguageName(selectedTargetLanguage))
        }
        startForegroundService(serviceIntent)
        isServiceStarted = true
        Toast.makeText(
            this@MainActivity,
            "Tap to translate\nTap around to drag",
            Toast.LENGTH_SHORT
        )
            .show()
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

