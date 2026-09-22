package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import com.example.ui.components.PulsingStatusDot
import com.example.ui.components.bounceClick
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.PrimaryIndigoDark
import com.example.ui.theme.SaffronGold
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoginScreen(
    loginError: String?,
    isOtpSent: Boolean = false,
    otpTargetMobile: String = "",
    otpTargetUser: UserEntity? = null,
    lastGeneratedOtp: String? = null,
    isOtpLoading: Boolean = false,
    onSendOtp: (mobileNumber: String) -> Unit = {},
    onVerifyOtp: (otpCode: String) -> Unit = {},
    onResendOtp: () -> Unit = {},
    onCancelOtp: () -> Unit = {},
    onLogin: (outletCode: String, credential: String) -> Unit,
    onQuickLogin: (UserRole) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedAuthMode by remember { mutableIntStateOf(0) } // 0 = Mobile OTP, 1 = Outlet Code + Password
    var mobileNumberInput by remember { mutableStateOf("") }
    var otpCodeInput by remember { mutableStateOf("") }

    var outletCode by remember { mutableStateOf("") }
    var passwordCredential by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    // Resend countdown timer (30 seconds)
    var resendCooldown by remember { mutableIntStateOf(30) }
    LaunchedEffect(isOtpSent) {
        if (isOtpSent) {
            resendCooldown = 30
            otpCodeInput = ""
            while (resendCooldown > 0) {
                delay(1000)
                resendCooldown--
            }
        }
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        PrimaryIndigoDark,
                        Color(0xFF0F172A),
                        Color(0xFF020617)
                    )
                )
            )
            .statusBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header Logo & Badge
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(EmeraldGreen, Color(0xFF059669))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "₹",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Outlet Ledger Portal",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )

            Text(
                text = "Secure Daily Purchases, Sales & Bank Deposits",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Main Authentication Card
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = Color(0xFF1E293B)
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    if (!isOtpSent) {
                        // Auth Mode Tabs: OTP vs Outlet Code
                        TabRow(
                            selectedTabIndex = selectedAuthMode,
                            containerColor = Color(0xFF0F172A),
                            contentColor = EmeraldGreen,
                            indicator = { tabPositions ->
                                TabRowDefaults.Indicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedAuthMode]),
                                    color = EmeraldGreen,
                                    height = 3.dp
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            Tab(
                                selected = selectedAuthMode == 0,
                                onClick = { selectedAuthMode = 0 },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Phone,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (selectedAuthMode == 0) EmeraldGreen else Color(0xFF94A3B8)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "Mobile OTP Login",
                                            fontWeight = if (selectedAuthMode == 0) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedAuthMode == 0) Color.White else Color(0xFF94A3B8)
                                        )
                                    }
                                },
                                modifier = Modifier.testTag("tab_mobile_otp")
                            )
                            Tab(
                                selected = selectedAuthMode == 1,
                                onClick = { selectedAuthMode = 1 },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Badge,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (selectedAuthMode == 1) EmeraldGreen else Color(0xFF94A3B8)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "Outlet Code",
                                            fontWeight = if (selectedAuthMode == 1) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedAuthMode == 1) Color.White else Color(0xFF94A3B8)
                                        )
                                    }
                                },
                                modifier = Modifier.testTag("tab_outlet_code")
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Error message banner with fluid entrance/exit
                    AnimatedVisibility(
                        visible = !loginError.isNullOrBlank(),
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF7F1D1D)
                            )
                        ) {
                            Text(
                                text = loginError ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFECACA),
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // -------------------------------------------------------------
                    // VIEW A: Active OTP Verification Step
                    // -------------------------------------------------------------
                    if (isOtpSent) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = onCancelOtp,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Change mobile number",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Verify OTP",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = "Sent to +91 ${otpTargetMobile.filter { it.isDigit() }.takeLast(10)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldGreen
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Target User summary card
                        if (otpTargetUser != null) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0F172A),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldGreen.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Security,
                                            contentDescription = null,
                                            tint = EmeraldGreen,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "${otpTargetUser.name} (${otpTargetUser.role})",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Code: ${otpTargetUser.outletCode}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        // OTP Notification Simulated Banner with fluid pulse and animation
                        AnimatedVisibility(
                            visible = !lastGeneratedOtp.isNullOrBlank(),
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = EmeraldGreen.copy(alpha = 0.15f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bounceClick()
                                    .clickable {
                                        otpCodeInput = lastGeneratedOtp ?: ""
                                    }
                                    .testTag("banner_simulated_otp")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    PulsingStatusDot(
                                        color = EmeraldGreen,
                                        size = 12.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Live OTP Generated",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = EmeraldGreen
                                        )
                                        Text(
                                            text = "Code is $lastGeneratedOtp (Tap to auto-fill)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = EmeraldGreen
                                    ) {
                                        Text(
                                            text = lastGeneratedOtp ?: "",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                fontFamily = FontFamily.Monospace
                                            ),
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // OTP Input Field
                        Text(
                            text = "Enter 6-Digit OTP",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFFE2E8F0)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = otpCodeInput,
                            onValueChange = { if (it.length <= 6) otpCodeInput = it.filter { ch -> ch.isDigit() } },
                            placeholder = { Text("6-digit code", color = Color(0xFF64748B)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Key,
                                    contentDescription = "OTP Code",
                                    tint = EmeraldGreen
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    keyboardController?.hide()
                                    if (otpCodeInput.length == 6) {
                                        onVerifyOtp(otpCodeInput)
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = EmeraldGreen,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_otp_input")
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Submit OTP Button
                        Button(
                            onClick = {
                                keyboardController?.hide()
                                onVerifyOtp(otpCodeInput)
                            },
                            enabled = otpCodeInput.length == 6,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .bounceClick()
                                .testTag("btn_verify_otp"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldGreen,
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFF334155),
                                disabledContentColor = Color(0xFF64748B)
                            )
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Verify & Login",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Resend OTP / Change Mobile row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Wrong number?",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier
                                    .clickable { onCancelOtp() }
                                    .padding(4.dp)
                            )

                            if (resendCooldown > 0) {
                                Text(
                                    text = "Resend OTP in ${resendCooldown}s",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8)
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable { onResendOtp() }
                                        .padding(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = SaffronGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Resend OTP",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = SaffronGold
                                    )
                                }
                            }
                        }
                    }

                    // -------------------------------------------------------------
                    // VIEW B: Mobile Number Input Mode (OTP Flow)
                    // -------------------------------------------------------------
                    else if (selectedAuthMode == 0) {
                        Text(
                            text = "Registered Mobile Number",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFFE2E8F0)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = mobileNumberInput,
                            onValueChange = {
                                if (it.length <= 15) {
                                    mobileNumberInput = it
                                }
                            },
                            placeholder = { Text("e.g. 8686122299, 9876543211", color = Color(0xFF64748B)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Phone,
                                    contentDescription = "Mobile Number",
                                    tint = EmeraldGreen
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    keyboardController?.hide()
                                    if (mobileNumberInput.filter { it.isDigit() }.length >= 10) {
                                        onSendOtp(mobileNumberInput)
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = EmeraldGreen,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_mobile_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Role Hints for quick testing
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0F172A),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Quick Mobile Selector:",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF94A3B8)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Admin Quick Fill (+91 8686122299)
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF3B82F6).copy(alpha = 0.2f),
                                        modifier = Modifier
                                            .clickable { mobileNumberInput = "8686122299" }
                                            .testTag("fill_admin_mobile")
                                    ) {
                                        Text(
                                            text = "Admin: 8686122299",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = Color(0xFF60A5FA),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }

                                    // RIC Quick Fill (9876543211)
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = SaffronGold.copy(alpha = 0.2f),
                                        modifier = Modifier
                                            .clickable { mobileNumberInput = "9876543211" }
                                            .testTag("fill_ric_mobile")
                                    ) {
                                        Text(
                                            text = "RIC: 9876543211",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = SaffronGold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }

                                    // EMP Quick Fill (9876543212)
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = EmeraldGreen.copy(alpha = 0.2f),
                                        modifier = Modifier
                                            .clickable { mobileNumberInput = "9876543212" }
                                            .testTag("fill_emp_mobile")
                                    ) {
                                        Text(
                                            text = "EMP: 9876543212",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = EmeraldGreen,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Send OTP Button
                        Button(
                            onClick = {
                                keyboardController?.hide()
                                onSendOtp(mobileNumberInput)
                            },
                            enabled = mobileNumberInput.filter { it.isDigit() }.length >= 10 && !isOtpLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .bounceClick()
                                .testTag("btn_send_otp"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldGreen,
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFF334155),
                                disabledContentColor = Color(0xFF64748B)
                            )
                        ) {
                            if (isOtpLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sending Security OTP...")
                            } else {
                                Icon(
                                    Icons.Default.Phone,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Send OTP Code",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Admin mobile is +91 8686122299. RIC & Employees mobile numbers are configured by Admin.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    // -------------------------------------------------------------
                    // VIEW C: Traditional Outlet Code + Password Mode
                    // -------------------------------------------------------------
                    else {
                        // Outlet Code Input
                        Text(
                            text = "Outlet Code",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFFE2E8F0)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = outletCode,
                            onValueChange = { outletCode = it },
                            placeholder = { Text("e.g. ADMIN, RIC01, EMP101", color = Color(0xFF64748B)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Badge,
                                    contentDescription = "Outlet Code",
                                    tint = EmeraldGreen
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = EmeraldGreen,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_outlet_code_input")
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Password Input
                        Text(
                            text = "Password (or Registered Mobile)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFFE2E8F0)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = passwordCredential,
                            onValueChange = { passwordCredential = it },
                            placeholder = { Text("Enter password or mobile number", color = Color(0xFF64748B)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "Password",
                                    tint = SaffronGold
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle password visibility",
                                        tint = Color(0xFF94A3B8)
                                    )
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    keyboardController?.hide()
                                    onLogin(outletCode, passwordCredential)
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = EmeraldGreen,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_password_input")
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Login Button
                        Button(
                            onClick = {
                                keyboardController?.hide()
                                onLogin(outletCode, passwordCredential)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .bounceClick()
                                .testTag("login_submit_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldGreen,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                Icons.Default.Key,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Login with Password",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Quick One-Tap Demo Logins for instant role testing
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_demo_login_section"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E293B).copy(alpha = 0.7f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(SaffronGold)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Quick Demo Access (One-Tap)",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = SaffronGold
                        )
                    }
                    Text(
                        text = "Instant login without typing for testing:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Admin Chip
                        OutlinedButton(
                            onClick = { onQuickLogin(UserRole.ADMIN) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF60A5FA)
                            ),
                            modifier = Modifier.testTag("demo_admin_login_chip")
                        ) {
                            Icon(
                                Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Admin (HQ)")
                        }

                        // RIC Chip
                        OutlinedButton(
                            onClick = { onQuickLogin(UserRole.RIC) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = SaffronGold
                            ),
                            modifier = Modifier.testTag("demo_ric_login_chip")
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("RIC In-Charge")
                        }

                        // Shop Employee Chip
                        OutlinedButton(
                            onClick = { onQuickLogin(UserRole.SHOP_EMPLOYEE) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = EmeraldGreen
                            ),
                            modifier = Modifier.testTag("demo_employee_login_chip")
                        ) {
                            Icon(
                                Icons.Default.Storefront,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Shop Employee")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Admin can set RIC & Employee mobile numbers in Shops & Staff management screen.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
    }
}
