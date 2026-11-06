package com.example.milo_2.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.milo_2.data.auth.AuthApiClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val MiloOrange = Color(0xFFFFB078)
private val MiloDark = Color(0xFF0A1422)
private val MiloWhite = Color(0xFFF7F7F7)
private val MiloGray = Color(0xFF6B7280)
private val MiloLightOrange = Color(0xFFFFF4ED)
private val MiloBorder = Color(0xFFE5E7EB)

@Composable
fun OtpVerificationScreen(
    destination: String,
    verificationType: String = "EMAIL",
    email: String = "",
    phoneNumber: String = "",
    onBack: () -> Unit = {},
    onVerificationSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val authClient = remember { AuthApiClient(context) }
    val coroutineScope = rememberCoroutineScope()

    var otpCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isResending by remember { mutableStateOf(false) }
    var resendCooldown by remember { mutableIntStateOf(60) }
    var devOtpPreview by remember { mutableStateOf<String?>(null) }

    val focusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()

    // Safely request focus after transition and layout have placed nodes
    LaunchedEffect(Unit) {
        delay(400)
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    // Resend countdown timer
    LaunchedEffect(Unit) {
        while (resendCooldown > 0) {
            delay(1000)
            resendCooldown--
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MiloWhite)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = "←",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MiloDark,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Title & Description
        Text(
            text = "Verify Code",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MiloDark,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        val channelLabel = if (verificationType.equals("PHONE", ignoreCase = true)) "phone number" else "email"
        Text(
            text = "Please enter the 6-digit verification code sent to your $channelLabel:",
            fontSize = 15.sp,
            color = MiloGray,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = destination.ifBlank { email.ifBlank { phoneNumber } },
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MiloDark,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Optional Dev Helper Chip (if available or tap to autofill)
        if (!devOtpPreview.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MiloLightOrange, shape = RoundedCornerShape(12.dp))
                    .border(1.dp, MiloOrange, shape = RoundedCornerShape(12.dp))
                    .clickable {
                        otpCode = devOtpPreview ?: ""
                        errorMessage = ""
                    }
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⚡ Dev Code: $devOtpPreview (Tap to autofill)",
                    color = MiloDark,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Hidden actual TextField that powers the custom 6-box UI
        BasicTextField(
            value = otpCode,
            onValueChange = { newValue ->
                val filtered = newValue.filter { it.isDigit() }
                if (filtered.length <= 6) {
                    otpCode = filtered
                    errorMessage = ""
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier
                .focusRequester(focusRequester)
                .size(1.dp)
                .alpha(0.01f)
        )

        // Custom 6-Digit OTP Boxes
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    try {
                        focusRequester.requestFocus()
                    } catch (_: Exception) {}
                },
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            for (i in 0 until 6) {
                val digit = if (i < otpCode.length) otpCode[i].toString() else ""
                val isFocused = otpCode.length == i || (otpCode.length == 6 && i == 5)

                Box(
                    modifier = Modifier
                        .size(width = 48.dp, height = 58.dp)
                        .background(Color.White, shape = RoundedCornerShape(12.dp))
                        .border(
                            width = if (isFocused) 2.dp else 1.dp,
                            color = if (isFocused) MiloOrange else MiloBorder,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = digit,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiloDark,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Status or Error Messages
        if (statusMessage.isNotEmpty()) {
            Text(
                text = statusMessage,
                color = Color(0xFF16A34A),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = Color.Red,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Verify & Continue Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(
                    color = if (isLoading || otpCode.length != 6) {
                        MiloOrange.copy(alpha = 0.5f)
                    } else {
                        MiloOrange
                    },
                    shape = RoundedCornerShape(18.dp)
                )
                .clickable(enabled = !isLoading && otpCode.length == 6) {
                    errorMessage = ""
                    statusMessage = ""
                    isLoading = true

                    coroutineScope.launch {
                        val result = authClient.verifyOtp(
                            otpCode = otpCode,
                            email = email.ifBlank { null },
                            phoneNumber = phoneNumber.ifBlank { null },
                            type = verificationType
                        )
                        isLoading = false
                        result.onSuccess {
                            statusMessage = "Verified successfully! Logging in..."
                            delay(400)
                            onVerificationSuccess()
                        }.onFailure { ex ->
                            errorMessage = ex.message ?: "Verification failed. Please try again."
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MiloDark,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Verify & Continue",
                    color = MiloDark,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Resend Code Link
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Didn't receive code? ",
                fontSize = 14.sp,
                color = MiloGray
            )

            if (resendCooldown > 0) {
                Text(
                    text = "Resend in ${resendCooldown}s",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MiloGray
                )
            } else {
                Text(
                    text = if (isResending) "Sending..." else "Resend Code",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiloOrange,
                    modifier = Modifier.clickable(enabled = !isResending) {
                        isResending = true
                        errorMessage = ""
                        coroutineScope.launch {
                            val res = authClient.sendOtp(
                                email = email.ifBlank { null },
                                phoneNumber = phoneNumber.ifBlank { null },
                                type = verificationType
                            )
                            isResending = false
                            res.onSuccess { data ->
                                statusMessage = "New OTP code sent!"
                                devOtpPreview = data.debugOtp
                                resendCooldown = 60
                            }.onFailure { ex ->
                                errorMessage = ex.message ?: "Failed to resend code."
                            }
                        }
                    }
                )
            }
        }
    }
}
