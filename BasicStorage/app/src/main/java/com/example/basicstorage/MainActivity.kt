package com.example.basicstorage

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.ComponentActivity
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : ComponentActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private val PREFS_NAME = "user_settings"
    private val KEY_USERNAME = "username"
    private val MEDIA_FILE_NAME = "saved_file.jpg"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        val editText: EditText = findViewById(R.id.editTextUsername)
        val saveSettingsButton: Button = findViewById(R.id.btnSaveSettings)
        val loadSettingsButton: Button = findViewById(R.id.btnLoadSettings)
        val saveImageButton: Button = findViewById(R.id.btnSaveImage)
        val loadImageButton: Button = findViewById(R.id.btnLoadImage)
        val imageView: ImageView = findViewById(R.id.imageView)

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        saveSettingsButton.setOnClickListener {
            val username = editText.text.toString()
            saveUserSettings(username)
        }

        loadSettingsButton.setOnClickListener {
            val username = loadUserSettings()
            editText.setText(username)
        }

        saveImageButton.setOnClickListener {
            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.sample_image) // save local sample image to internal storage
            saveImageToInternalStorage(bitmap)
        }

        loadImageButton.setOnClickListener {
            val bitmap = loadImageFromInternalStorage() // load from internal storage
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap)
            }
        }
    }

    private fun saveUserSettings(username: String) {
        val editor = sharedPreferences.edit()
        editor.putString(KEY_USERNAME, username)
        editor.apply()
    }

    private fun loadUserSettings(): String {
        return sharedPreferences.getString(KEY_USERNAME, "Default User") ?: "Default User"
    }

    private fun saveImageToInternalStorage(bitmap: Bitmap) {
        val file = File(filesDir, MEDIA_FILE_NAME)
        try {
            val fileOutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
            fileOutputStream.close()
        } catch (e: IOException) {
            Log.e("MainActivity", "Failed to save image: ${e.message}", e)
        }
    }

    private fun loadImageFromInternalStorage(): Bitmap? {
        val file = File(filesDir, MEDIA_FILE_NAME)
        return if (file.exists()) {
            BitmapFactory.decodeFile(file.absolutePath)
        } else {
            Log.e("MainActivity", "Image file does not exist")
            null
        }
    }
}