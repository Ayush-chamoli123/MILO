package com.example.milo_2.screens.auth

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
    onLoginSuccess: () -> Unit = {},
    onNavigateToOtp: (destination: String, type: String, email: String, phone: String) -> Unit = { _, _, _, _ -> }
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

    var isOtpMode by remember {
        mutableStateOf(false)
    }

    var isPasswordVisible by remember {
        mutableStateOf(false)
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

    // ====================================================
    // MAIN SCREEN
    // ====================================================

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
                    .padding(4.dp)
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        // =================================================
        // TITLE
        // =================================================

        Text(
            text = if (isOtpMode) "Sign In with OTP" else "Log In",
            color = MiloDark,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = if (isOtpMode)
                "Enter your email or phone number to receive a 6-digit verification code."
            else
                "Welcome back! Enter your details to continue.",
            color = MiloGray,
            fontSize = 15.sp
        )

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        // =================================================
        // EMAIL / USERNAME FIELD
        // =================================================

        Text(
            text = if (isOtpMode) "Email or Phone Number" else "Email or Username",
            color = MiloDark,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 4.dp,
                    bottom = 8.dp
                )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color(0xFFE5E7EB),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(
                    horizontal = 16.dp
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = emailOrUsername,
                onValueChange = {
                    emailOrUsername = it
                    errorMessage = ""
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (isOtpMode) KeyboardType.Email else KeyboardType.Text
                ),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 16.sp,
                    color = MiloDark
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (emailOrUsername.isEmpty()) {
                Text(
                    text = if (isOtpMode) "e.g. user@gmail.com or +15551234567" else "Enter your email or username",
                    color = Color(0xFF9CA3AF),
                    fontSize = 15.sp
                )
            }
        }

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        // =================================================
        // PASSWORD FIELD (Only in password mode)
        // =================================================

        if (!isOtpMode) {
            Text(
                text = "Password",
                color = MiloDark,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 4.dp,
                        bottom = 8.dp
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0xFFE5E7EB),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(
                        horizontal = 16.dp
                    ),
                contentAlignment = Alignment.CenterStart
            ) {

                BasicTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = ""
                    },
                    singleLine = true,
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 16.sp,
                        color = MiloDark
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (password.isEmpty()) {
                    Text(
                        text = "Enter your password",
                        color = Color(0xFF9CA3AF),
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Switch to OTP mode link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Sign in with OTP code instead",
                    color = MiloOrange,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable {
                            isOtpMode = true
                            errorMessage = ""
                        }
                        .padding(4.dp)
                )
            }
        } else {
            // Switch back to Password mode link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = "← Use password to sign in",
                    color = MiloDark,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable {
                            isOtpMode = false
                            errorMessage = ""
                        }
                        .padding(4.dp)
                )
            }
        }

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
                        bottom = 12.dp
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

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        // =================================================
        // PRIMARY ACTION BUTTON
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

                    if (!isOtpMode && password.isBlank()) {
                        errorMessage = "Please enter your password"
                        return@clickable
                    }

                    errorMessage = ""
                    isLoading = true
                    keyboardController?.hide()
                    focusManager.clearFocus()

                    coroutineScope.launch {
                        if (isOtpMode) {
                            // Passwordless OTP Login
                            val isPhone = emailOrUsername.trim().startsWith("+") || emailOrUsername.trim().all { it.isDigit() || it == '-' || it == ' ' }
                            val channel = if (isPhone) "PHONE" else "EMAIL"
                            val sendResult = authClient.sendOtp(
                                email = if (!isPhone) emailOrUsername.trim() else null,
                                phoneNumber = if (isPhone) emailOrUsername.trim() else null,
                                type = channel
                            )
                            isLoading = false
                            sendResult.onSuccess { otpData ->
                                onNavigateToOtp(
                                    otpData.destination,
                                    otpData.type,
                                    if (!isPhone) emailOrUsername.trim() else "",
                                    if (isPhone) emailOrUsername.trim() else ""
                                )
                            }.onFailure { ex ->
                                errorMessage = ex.message ?: "Failed to dispatch OTP code. Please check your details."
                            }
                        } else {
                            // Standard Password Login
                            val result = authClient.login(
                                emailOrUsername = emailOrUsername.trim(),
                                password = password
                            )
                            isLoading = false
                            result.onSuccess { authData ->
                                if (authData.requiresOtp) {
                                    val dest = authData.maskedDestination ?: emailOrUsername.trim()
                                    val channel = authData.otpChannel ?: "EMAIL"
                                    onNavigateToOtp(
                                        dest,
                                        channel,
                                        if (channel == "EMAIL") emailOrUsername.trim() else "",
                                        if (channel == "PHONE") emailOrUsername.trim() else ""
                                    )
                                } else {
                                    onLoginSuccess()
                                }
                            }.onFailure { ex ->
                                errorMessage = ex.message ?: "Authentication failed. Please try again."
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isLoading) {
                    if (isOtpMode) "Sending Code..." else "Logging in..."
                } else {
                    if (isOtpMode) "Send Verification Code" else "Log In"
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
        // SIGNUP LINK
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
