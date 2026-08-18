package com.example.milo_2.screens.auth

import com.google.firebase.auth.FirebaseAuth
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


// ========================================================
// MILO COLORS
// ========================================================

private val MiloOrange = Color(0xFFFFB078)
private val MiloDark = Color(0xFF0A1422)
private val MiloWhite = Color(0xFFF7F7F7)
private val MiloGray = Color(0xFF6B7280)


// ========================================================
// SIGNUP SCREEN
// ========================================================

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
fun SignupScreen(
    onBack: () -> Unit = {},
    onLogin: () -> Unit = {},
    onSignupSuccess: () -> Unit = {}
) {

    var name by remember {
        mutableStateOf("")
    }

    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var confirmPassword by remember {
        mutableStateOf("")
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    // Firebase Authentication
    val auth = remember {
        FirebaseAuth.getInstance()
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MiloWhite)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(
                start = 28.dp,
                end = 28.dp,
                top = 20.dp,
                bottom = 20.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // =================================================
        // BACK BUTTON
        // =================================================

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {

            Text(
                text = "←",
                color = MiloDark,
                fontSize = 30.sp,
                modifier = Modifier
                    .clickable {
                        onBack()
                    }
                    .padding(horizontal = 5.dp)
            )
        }


        Spacer(
            modifier = Modifier.height(20.dp)
        )


        // =================================================
        // TITLE
        // =================================================

        Text(
            text = "Welcome to MILO",
            color = MiloDark,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )


        Spacer(
            modifier = Modifier.height(8.dp)
        )


        Text(
            text = "Create your account and start\npooling with people.",
            color = MiloGray,
            fontSize = 16.sp,
            lineHeight = 22.sp
        )


        Spacer(
            modifier = Modifier.height(32.dp)
        )


        // =================================================
        // NAME
        // =================================================

        MiloTextField(
            value = name,
            onValueChange = {
                name = it
                errorMessage = ""
            },
            label = "Full Name",
            placeholder = "Enter your name"
        )


        Spacer(
            modifier = Modifier.height(16.dp)
        )


        // =================================================
        // EMAIL
        // =================================================

        MiloTextField(
            value = email,
            onValueChange = {
                email = it
                errorMessage = ""
            },
            label = "Email",
            placeholder = "Enter your email"
        )


        Spacer(
            modifier = Modifier.height(16.dp)
        )


        // =================================================
        // PASSWORD
        // =================================================

        MiloTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = ""
            },
            label = "Password",
            placeholder = "Create a password",
            isPassword = true
        )


        Spacer(
            modifier = Modifier.height(16.dp)
        )


        // =================================================
        // CONFIRM PASSWORD
        // =================================================

        MiloTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                errorMessage = ""
            },
            label = "Confirm Password",
            placeholder = "Re-enter your password",
            isPassword = true
        )


        Spacer(
            modifier = Modifier.height(18.dp)
        )


        // =================================================
        // ERROR MESSAGE
        // =================================================

        if (errorMessage.isNotEmpty()) {

            Text(
                text = errorMessage,
                color = Color.Red,
                fontSize = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 4.dp,
                        bottom = 10.dp
                    )
            )
        }


        // =================================================
        // CREATE ACCOUNT
        // =================================================

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(
                    color = if (isLoading) {
                        MiloOrange.copy(alpha = 0.6f)
                    } else {
                        MiloOrange
                    },
                    shape = RoundedCornerShape(18.dp)
                )
                .clickable(enabled = !isLoading) {

                    // -------------------------------------
                    // BASIC VALIDATION
                    // -------------------------------------

                    if (name.isBlank()) {
                        errorMessage = "Please enter your name"
                        return@clickable
                    }

                    if (email.isBlank()) {
                        errorMessage = "Please enter your email"
                        return@clickable
                    }

                    if (password.length < 6) {
                        errorMessage =
                            "Password must be at least 6 characters"
                        return@clickable
                    }

                    if (password != confirmPassword) {
                        errorMessage =
                            "Passwords do not match"
                        return@clickable
                    }


                    // -------------------------------------
                    // START FIREBASE SIGNUP
                    // -------------------------------------

                    errorMessage = ""
                    isLoading = true

                    auth.createUserWithEmailAndPassword(
                        email.trim(),
                        password
                    ).addOnCompleteListener { task ->

                        isLoading = false

                        if (task.isSuccessful) {

                            // Account successfully created
                            onSignupSuccess()

                        } else {

                            errorMessage =
                                task.exception?.message
                                    ?: "Account creation failed"
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = if (isLoading) {
                    "Creating Account..."
                } else {
                    "Create Account"
                },
                color = MiloDark,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }


        Spacer(
            modifier = Modifier.height(22.dp)
        )


        // =================================================
        // LOGIN
        // =================================================

        Text(
            text = "Already have an account?  Log in",
            color = MiloDark,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clickable {
                    onLogin()
                }
                .padding(8.dp)
        )
    }
}

// ========================================================
// MILO TEXT FIELD
// ========================================================

@Composable
private fun MiloTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    isPassword: Boolean = false
) {

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        Text(
            text = label,
            color = MiloDark,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(
                start = 4.dp,
                bottom = 7.dp
            )
        )


        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = placeholder,
                    color = MiloGray.copy(alpha = 0.7f)
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            visualTransformation = if (isPassword) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            }
        )
    }
}