package com.example.statemanagement

import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {

    private val userInputText = ""

    private lateinit var editText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        editText = findViewById(R.id.editText)
    }

    override fun onSaveInstanceState(outState: Bundle) { // called before onStop()
        super.onSaveInstanceState(outState)

        outState.putString(userInputText, editText.text.toString())
        Toast.makeText(this, "State saved in onSaveInstanceState!", Toast.LENGTH_SHORT).show()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) { // called after onStart()
        super.onRestoreInstanceState(savedInstanceState)

        val savedText = savedInstanceState.getString(userInputText, "")
        editText.setText(savedText)
        Toast.makeText(this, "State restored from onRestoreInstanceState!", Toast.LENGTH_SHORT).show()
    }
}