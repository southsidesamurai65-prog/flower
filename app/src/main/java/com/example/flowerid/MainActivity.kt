package com.example.flowerid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.flowerid.ui.MainScaffold
import com.example.flowerid.ui.theme.FlowerIdTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlowerIdTheme {
                MainScaffold()
            }
        }
    }
}
