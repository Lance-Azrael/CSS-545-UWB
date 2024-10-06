package com.example.helloworld

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.helloworld.ui.theme.HelloWorldTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            HelloWorldTheme {
                Greeting(
                    modifier = Modifier
                )
            }
        }
    }

    fun jump() {
        val intent = Intent(this, NextActivity::class.java)
        startActivity(intent)
    }

    @Composable
    fun Greeting(modifier: Modifier = Modifier) {

        Surface(color = Color.Cyan) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Hello!",
                    fontSize = 100.sp,
                    color = Color.Red,
                    modifier = modifier
                )
                Button(colors = ButtonDefaults.buttonColors(Color.LightGray), onClick = { jump() }) {
                    Text(
                        text = "next",
                        fontSize = 30.sp,
                        color = Color.Blue,
                        modifier = Modifier.padding(5.dp)
                    )
                }
            }
        }
    }
}


//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    HelloWorldTheme {
//        Greeting("Android", Modifier.padding(24.dp))
//    }
//}