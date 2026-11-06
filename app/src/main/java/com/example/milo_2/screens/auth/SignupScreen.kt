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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
    onSignupSuccess: () -> Unit = {},
    onNavigateToOtp: (destination: String, type: String, email: String, phone: String) -> Unit = { _, _, _, _ -> }
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val authClient = remember { AuthApiClient(context) }
    val sessionManager = remember { SessionManager(context) }

    var name by remember {
        mutableStateOf("")
    }

    var username by remember {
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

    var showServerDialog by remember {
        mutableStateOf(false)
    }

    var serverUrlInput by remember {
        mutableStateOf(sessionManager.baseUrl)
    }

    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MiloWhite)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(scrollState)
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
            modifier = Modifier.height(16.dp)
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
            modifier = Modifier.height(24.dp)
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
            placeholder = "Enter your full name"
        )

        Spacer(
            modifier = Modifier.height(14.dp)
        )

        // =================================================
        // USERNAME
        // =================================================

        MiloTextField(
            value = username,
            onValueChange = {
                username = it
                errorMessage = ""
            },
            label = "Username",
            placeholder = "Choose a unique username"
        )

        Spacer(
            modifier = Modifier.height(14.dp)
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
            modifier = Modifier.height(14.dp)
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
            placeholder = "Minimum 8 characters",
            isPassword = true
        )

        Spacer(
            modifier = Modifier.height(14.dp)
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
            modifier = Modifier.height(16.dp)
        )

        // =================================================
        // ERROR MESSAGE
        // =================================================

        if (errorMessage.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 4.dp,
                        bottom = 10.dp
                    )
            ) {
                Text(
                    text = errorMessage,
                    color = Color.Red,
                    fontSize = 14.sp
                )
                if (errorMessage.contains("Network error")) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "🔄 Test Backend Connection",
                        color = MiloDark,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable {
                                coroutineScope.launch {
                                    val isAlive = authClient.checkHealth()
                                    if (isAlive) {
                                        errorMessage = ""
                                    } else {
                                        errorMessage = "Backend still not responding at ${sessionManager.baseUrl}. Ensure FastAPI is started on your computer."
                                    }
                                }
                            }
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }

        // =================================================
        // CREATE ACCOUNT BUTTON
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
                    if (name.isBlank()) {
                        errorMessage = "Please enter your name"
                        return@clickable
                    }

                    if (username.isBlank() || username.length < 3) {
                        errorMessage = "Username must be at least 3 characters"
                        return@clickable
                    }

                    if (email.isBlank() || !email.contains("@")) {
                        errorMessage = "Please enter a valid email"
                        return@clickable
                    }

                    if (password.length < 8) {
                        errorMessage = "Password must be at least 8 characters"
                        return@clickable
                    }

                    if (password != confirmPassword) {
                        errorMessage = "Passwords do not match"
                        return@clickable
                    }

                    errorMessage = ""
                    isLoading = true
                    keyboardController?.hide()
                    focusManager.clearFocus()

                    coroutineScope.launch {
                        val result = authClient.register(
                            fullName = name.trim(),
                            username = username.trim(),
                            email = email.trim(),
                            password = password
                        )
                        isLoading = false
                        result.onSuccess { authData ->
                            if (authData.requiresOtp) {
                                val dest = authData.maskedDestination ?: email.trim()
                                val channel = authData.otpChannel ?: "EMAIL"
                                onNavigateToOtp(dest, channel, email.trim(), "")
                            } else {
                                onSignupSuccess()
                            }
                        }.onFailure { ex ->
                            errorMessage = ex.message ?: "Account creation failed. Please try again."
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
            modifier = Modifier.height(24.dp)
        )

        // =================================================
        // LOGIN LINK
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

        Spacer(modifier = Modifier.height(16.dp))

        // Backend URL indicator
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
// TEXT FIELD COMPONENT
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