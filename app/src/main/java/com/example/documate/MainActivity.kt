package com.example.documate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.documate.core.navigation.DocuNavGraph
import com.example.documate.core.theme.DocuMateTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DocuMateTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DocuNavGraph()
                }
            }
        }
    }
}
