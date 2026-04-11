package com.methodica.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge hace que el contenido se extienda bajo las barras del sistema;
        // el Scaffold de MethodicaApp gestiona el padding con WindowInsets.
        enableEdgeToEdge()
        setContent {
            MethodicaApp()
        }
    }
}
