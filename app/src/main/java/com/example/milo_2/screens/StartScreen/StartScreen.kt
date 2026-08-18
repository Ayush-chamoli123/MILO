package com.example.milo_2.screens.StartScreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.milo_2.R
import kotlinx.coroutines.delay
@Preview(showBackground = true)
@Composable
fun StartScreen(
    onFinished: () -> Unit = {}
) {

    LaunchedEffect(Unit) {

        delay(3000)

        onFinished()
    }
    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        // Background
        Image(
            painter = painterResource(id = R.drawable.ic_start),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Logo + Text
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-20).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // MILO Logo
            Image(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = "MILO Logo",
                modifier = Modifier.size(150.dp)
            )

            // MILO text
            Text(
                text = "MILO",
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 6.sp,
                modifier = Modifier.padding(top = 12.dp)
            )

            // Tagline
            Text(
                text = "Find People. Do More Together.",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}