package com.example.milo_2.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.milo_2.data.auth.AuthApiClient
import com.example.milo_2.data.auth.SessionManager
import kotlinx.coroutines.launch


// ========================================================
// MILO COLORS
// ========================================================

private val MiloOrange = Color(0xFFFFB078)
private val MiloDark = Color(0xFF0A1422)
private val MiloWhite = Color(0xFFF7F7F7)
private val MiloGray = Color(0xFF6B7280)


// ========================================================
// LOGIN SCREEN
// ========================================================

@Composable
fun LoginScreen(
    onBack: () -> Unit = {},
    onSignup: () -> Unit = {},
    onLoginSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val authClient = remember { AuthApiClient(context) }
    val sessionManager = remember { SessionManager(context) }

    // ====================================================
    // STATES
    // ====================================================

    var emailOrUsername by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    var showServerDialog by remember {
        mutableStateOf(false)
    }

    var serverUrlInput by remember {
        mutableStateOf(sessionManager.baseUrl)
    }

    // ====================================================
    // MAIN SCREEN
    // ====================================================

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
            modifier = Modifier.height(30.dp)
        )

        // =================================================
        // TITLE
        // =================================================

        Text(
            text = "Welcome Back",
            color = MiloDark,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Log in to continue pooling\nwith people.",
            color = MiloGray,
            fontSize = 16.sp,
            lineHeight = 22.sp
        )

        Spacer(
            modifier = Modifier.height(40.dp)
        )

        // =================================================
        // EMAIL OR USERNAME
        // =================================================

        MiloLoginTextField(
            value = emailOrUsername,
            onValueChange = {
                emailOrUsername = it
                errorMessage = ""
            },
            label = "Email or Username",
            placeholder = "Enter your email or username"
        )

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        // =================================================
        // PASSWORD
        // =================================================

        MiloLoginTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = ""
            },
            label = "Password",
            placeholder = "Enter your password",
            isPassword = true
        )

        Spacer(
            modifier = Modifier.height(14.dp)
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
                        bottom = 12.dp
                    )
            )
        }

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        // =================================================
        // LOGIN BUTTON
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
                .clickable(
                    enabled = !isLoading
                ) {
                    if (emailOrUsername.isBlank()) {
                        errorMessage = "Please enter your email or username"
                        return@clickable
                    }

                    if (password.isBlank()) {
                        errorMessage = "Please enter your password"
                        return@clickable
                    }

                    errorMessage = ""
                    isLoading = true

                    coroutineScope.launch {
                        val result = authClient.login(
                            emailOrUsername = emailOrUsername.trim(),
                            password = password
                        )
                        isLoading = false
                        result.onSuccess {
                            onLoginSuccess()
                        }.onFailure { ex ->
                            errorMessage = ex.message ?: "Authentication failed. Please try again."
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isLoading) {
                    "Logging in..."
                } else {
                    "Log In"
                },
                color = MiloDark,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        // =================================================
        // SIGNUP
        // =================================================

        Text(
            text = "Don't have an account?  Sign up",
            color = MiloDark,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clickable {
                    onSignup()
                }
                .padding(8.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        // =================================================
        // BACKEND SERVER URL CONFIG
        // =================================================
        Row(
            modifier = Modifier
                .clickable {
                    serverUrlInput = sessionManager.baseUrl
                    showServerDialog = true
                }
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Backend: ${sessionManager.baseUrl} ⚙️",
                color = MiloGray,
                fontSize = 12.sp
            )
        }
    }

    if (showServerDialog) {
        AlertDialog(
            onDismissRequest = { showServerDialog = false },
            title = { Text(text = "Configure Backend Server") },
            text = {
                Column {
                    Text(
                        text = "Use 10.0.2.2 for Android Emulator, or your computer's local IP (e.g. 10.115.219.129) for physical devices.",
                        fontSize = 13.sp,
                        color = MiloGray
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = serverUrlInput,
                        onValueChange = { serverUrlInput = it },
                        label = { Text("Server URL") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (serverUrlInput.isNotBlank()) {
                            sessionManager.baseUrl = serverUrlInput
                        }
                        showServerDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showServerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}


// ========================================================
// MILO LOGIN TEXT FIELD
// ========================================================

@Composable
private fun MiloLoginTextField(
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
                    color = MiloGray.copy(
                        alpha = 0.7f
                    )
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


// ========================================================
// LOGIN PREVIEW
// ========================================================

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun LoginScreenPreview() {
    LoginScreen()
}