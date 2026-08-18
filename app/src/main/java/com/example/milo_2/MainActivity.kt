package com.example.milo_2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.milo_2.navigation.MiloNavGraph
import com.example.milo_2.ui.theme.MILO_2Theme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            MILO_2Theme {

                MiloNavGraph()

            }
        }
    }
}