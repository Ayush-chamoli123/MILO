package com.example.milo_2.screens.welcomescreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.milo_2.R


// ========================================================
// MILO COLORS
// ========================================================

private val MiloOrange = Color(0xFFFFB078)
private val MiloWhite = Color(0xFFF7F7F7)
private val MiloDark = Color(0xFF0A1422)


// ========================================================
// WELCOME SCREEN
// ========================================================

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
fun WelcomeScreen(
    onLogin: () -> Unit = {},
    onSignup: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        // =================================================
        // BACKGROUND IMAGE
        // =================================================

        Image(
            painter = painterResource(
                id = R.drawable.img
            ),
            contentDescription = "MILO background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.BottomCenter
        )


        // =================================================
        // DARK OVERLAY
        // =================================================

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.80f),
                            Color.Black.copy(alpha = 0.70f),
                            Color.Black.copy(alpha = 0.18f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.78f)
                        )
                    )
                )
        )


        // =================================================
        // MAIN CONTENT
        // =================================================

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(
                    start = 50.dp,
                    end = 40.dp,
                    top = 50.dp,
                    bottom = 18.dp
                ),
            horizontalAlignment = Alignment.Start
        ) {

            // =================================================
            // HEADLINE
            // =================================================

            Text(
                text = "Find People.",
                color = MiloWhite,
                fontSize = 35.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 45.sp
            )

            Text(
                text = "Do Things.",
                color = MiloWhite,
                fontSize = 35.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 45.sp
            )

            Text(
                text = "Create Moments.",
                color = MiloOrange,
                fontSize = 35.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 45.sp
            )


            // =================================================
            // GAP AFTER HEADLINE
            // =================================================

            Spacer(
                modifier = Modifier.height(11.dp)
            )


            // =================================================
            // DESCRIPTION
            // =================================================

            Text(
                text = "Post what you need. Find people\nnearby. Pool together and make\nit happen.",
                color = Color.White.copy(alpha = 0.90f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 20.sp
            )


            // =================================================
            // PUSH LOWER CONTENT DOWN
            // =================================================

            Spacer(
                modifier = Modifier.weight(3f)
            )


            // =================================================
            // FEATURES
            // =================================================

            FeatureSection()


            // =================================================
            // GAP BEFORE BUTTON
            // =================================================

            Spacer(
                modifier = Modifier.height(9.dp)
            )


            // =================================================
            // GET STARTED BUTTON
            // =================================================

            GetStartedButton(
                onClick = onSignup
            )


            // =================================================
            // GAP BEFORE LOGIN
            // =================================================

            Spacer(
                modifier = Modifier.height(24.dp)
            )


            // =================================================
            // LOGIN
            // =================================================

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onLogin()
                    },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "Already have an account? ",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 17.sp
                )

                Text(
                    text = "Log in",
                    color = MiloOrange,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


// ========================================================
// FEATURE SECTION
// ========================================================

@Composable
private fun FeatureSection() {

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {

        FeatureItem(
            modifier = Modifier.weight(1f),
            icon = R.drawable.ic_location,
            title = "Near You"
        )

        FeatureDivider()

        FeatureItem(
            modifier = Modifier.weight(1f),
            icon = R.drawable.ic_people,
            title = "Verified\nPeople"
        )

        FeatureDivider()

        FeatureItem(
            modifier = Modifier.weight(1f),
            icon = R.drawable.ic_handshake,
            title = "Trusted\nPooling"
        )

        FeatureDivider()

        FeatureItem(
            modifier = Modifier.weight(1f),
            icon = R.drawable.ic_shield,
            title = "Safe &\nSecure"
        )
    }
}


// ========================================================
// FEATURE ITEM
// ========================================================

@Composable
private fun FeatureItem(
    modifier: Modifier,
    icon: Int,
    title: String
) {

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // -----------------------------------------------
        // ICON CIRCLE
        // -----------------------------------------------

        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(
                    Color.Black.copy(alpha = 0.30f)
                ),
            contentAlignment = Alignment.Center
        ) {

            Image(
                painter = painterResource(id = icon),
                contentDescription = title,
                modifier = Modifier.size(34.dp)
            )
        }


        // -----------------------------------------------
        // GAP
        // -----------------------------------------------

        Spacer(
            modifier = Modifier.height(12.dp)
        )


        // -----------------------------------------------
        // TITLE
        // -----------------------------------------------

        Text(
            text = title,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center
        )
    }
}


// ========================================================
// FEATURE DIVIDER
// ========================================================

@Composable
private fun FeatureDivider() {

    Box(
        modifier = Modifier
            .padding(
                top = 0.dp
            )
            .size(
                width = 1.dp,
                height = 105.dp
            )
            .background(
                Color.White.copy(alpha = 0.35f)
            )
    )
}


// ========================================================
// GET STARTED BUTTON
// ========================================================

@Composable
private fun GetStartedButton(
    onClick: () -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .clip(
                RoundedCornerShape(25.dp)
            )
            .background(MiloOrange)
            .clickable {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "Get Started",
                textAlign = TextAlign.Center,
                color = MiloDark,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.size(1.dp)
            )


        }
    }
}