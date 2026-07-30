package com.tollscan.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tollscan.app.ui.navigation.TollScanNavGraph
import com.tollscan.app.ui.theme.TollScanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TollScanTheme {
                TollScanNavGraph()
            }
        }
    }
}
