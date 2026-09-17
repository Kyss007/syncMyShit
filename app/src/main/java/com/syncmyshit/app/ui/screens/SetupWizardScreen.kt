package com.syncmyshit.app.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.syncmyshit.app.SyncApplication
import com.syncmyshit.app.ui.components.dpadFocusable
import com.syncmyshit.app.ui.theme.DarkBackground
import com.syncmyshit.app.ui.theme.DarkBorder
import com.syncmyshit.app.ui.theme.DarkSurface
import com.syncmyshit.app.ui.theme.DarkSurfaceElevated
import com.syncmyshit.app.ui.theme.NeonCyan
import com.syncmyshit.app.ui.theme.NeonPurple
import com.syncmyshit.app.ui.theme.StatusGreen
import com.syncmyshit.app.ui.theme.StatusRed
import com.syncmyshit.app.ui.theme.StatusYellow
import com.syncmyshit.app.ui.theme.TextMuted
import com.syncmyshit.app.ui.theme.TextPrimary
import com.syncmyshit.app.ui.theme.TextSecondary
import com.syncmyshit.app.ui.viewmodel.SetupViewModel
import com.syncmyshit.app.utils.SigningUtils
import com.syncmyshit.app.utils.StorageAccessHelper

@Composable
fun SetupWizardScreen(
    viewModel: SetupViewModel,
    onSetupComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentStep by viewModel.currentStep.collectAsState()
    val hasStoragePermission by viewModel.hasStoragePermission.collectAsState()
    val hasUsagePermission by viewModel.hasUsagePermission.collectAsState()
    val signedInAccount by viewModel.signedInAccount.collectAsState()
    val signedInEmail by viewModel.signedInEmail.collectAsState(initial = null)
    val authError by viewModel.authErrorMessage.collectAsState()
    val customClientId by viewModel.customOAuthClientId.collectAsState("")
    val discoveredList by viewModel.discoveredEmulators.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    val isConnected = !signedInEmail.isNullOrBlank() || signedInAccount != null
    val currentEmail = signedInEmail ?: signedInAccount?.email ?: ""

    var customIdInput by remember(customClientId) { mutableStateOf(customClientId) }
    var showCustomOAuthConfig by remember { mutableStateOf(false) }

    // Re-check permissions when returning from settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
                viewModel.checkAuth()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Google Sign In Launcher
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                viewModel.onSignInSuccess(account)
            }
        } catch (e: ApiException) {
            val reason = when (e.statusCode) {
                10 -> "Google Developer Error 10 (DEVELOPER_ERROR): This APK's SHA-1 certificate is not registered in Google Cloud Console OAuth 2.0 Credentials. Follow the 1-minute guide below to link it."
                12500 -> "Google Sign-In failed (Code 12500). Please check your internet connection or Google Play Services."
                12501 -> "Sign-in was cancelled."
                12502 -> "Sign-in currently in progress."
                7 -> "Network error: Unable to contact Google authentication servers."
                else -> "Google Sign-In error (Code ${e.statusCode}): ${e.message ?: "Authentication failed"}"
            }
            viewModel.setAuthError(reason)
        } catch (e: Exception) {
            viewModel.setAuthError("Sign-in error: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // App Branding
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "syncMyShit",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = NeonCyan
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Automagic Save Sync Setup",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )
        }

        // Stepper Progress Indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepIndicator(step = 1, currentStep = currentStep, label = "Permissions")
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.width(30.dp).height(2.dp).background(if (currentStep > 1) NeonCyan else DarkBorder))
            Spacer(modifier = Modifier.width(8.dp))
            StepIndicator(step = 2, currentStep = currentStep, label = "Drive")
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.width(30.dp).height(2.dp).background(if (currentStep > 2) NeonCyan else DarkBorder))
            Spacer(modifier = Modifier.width(8.dp))
            StepIndicator(step = 3, currentStep = currentStep, label = "Discovery")
        }

        // STEP 1: Permissions
        if (currentStep == 1) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Step 1: Grant Permissions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "To automagically find save files and detect when you start or finish playing, syncMyShit requires two standard Android permissions:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    // Permission 1: All Files Access
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkBackground),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = if (hasStoragePermission) StatusGreen else NeonCyan,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "All Files Access",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Required to read & write saves in emulator directories across internal storage and SD cards.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            if (hasStoragePermission) {
                                Icon(Icons.Default.CheckCircle, null, tint = StatusGreen, modifier = Modifier.size(24.dp))
                            } else {
                                Button(
                                    onClick = { context.startActivity(StorageAccessHelper.getAllFilesAccessIntent(context)) },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                                ) {
                                    Text("Grant", color = DarkSurface)
                                }
                            }
                        }
                    }

                    // Permission 2: Usage Access
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkBackground),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Games,
                                contentDescription = null,
                                tint = if (hasUsagePermission) StatusGreen else NeonPurple,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Usage Stats Access",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Detects when you open or exit games so it can sync before and after playing automagically.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            if (hasUsagePermission) {
                                Icon(Icons.Default.CheckCircle, null, tint = StatusGreen, modifier = Modifier.size(24.dp))
                            } else {
                                Button(
                                    onClick = { context.startActivity(StorageAccessHelper.getUsageStatsSettingsIntent()) },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                                ) {
                                    Text("Grant", color = DarkSurface)
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { viewModel.nextStep() },
                        enabled = hasStoragePermission,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Text("Continue to Step 2", color = DarkSurface, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, null, tint = DarkSurface)
                    }
                }
            }
        }

        // STEP 2: Google Drive
        if (currentStep == 2) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Step 2: Connect Google Drive",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Your save files will be kept in a private folder named 'syncMyShit' on your Google Drive. Everything is encrypted and private to your account.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    if (isConnected) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkBackground),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, null, tint = StatusGreen, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Connected Successfully", fontWeight = FontWeight.Bold, color = StatusGreen)
                                    Text(currentEmail, color = TextSecondary)
                                }
                            }
                        }
                    } else {
                        // Option 1 (Recommended / Universal): Web Browser Login
                        Button(
                            onClick = {
                                viewModel.setAuthError(null)
                                val activeClientId = customIdInput.ifBlank { customClientId.ifBlank { null } }
                                viewModel.startWebLogin(activeClientId) {
                                    showCustomOAuthConfig = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                        ) {
                            Icon(Icons.Default.Language, null, tint = DarkSurface)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Sign In via Web Browser (Universal / GammaOS)", color = DarkSurface, fontWeight = FontWeight.Bold)
                        }

                        // Option 2 (GMS 1-Tap): Google Play Services
                        OutlinedButton(
                            onClick = {
                                viewModel.setAuthError(null)
                                val app = context.applicationContext as SyncApplication
                                val intent = app.authManager.getSignInIntent(customClientId.ifBlank { null })
                                signInLauncher.launch(intent)
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                        ) {
                            Icon(Icons.Default.Cloud, null, modifier = Modifier.size(18.dp), tint = TextSecondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign In with Google Play Services (GMS)")
                        }

                        // Auth Error Alert & Troubleshooting
                        if (authError != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkBackground),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Warning, null, tint = StatusYellow, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Google Sign-In Needs Credentials", fontWeight = FontWeight.Bold, color = StatusYellow)
                                    }

                                    Text(
                                        text = authError!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )

                                    val currentSha1 = remember { SigningUtils.getAppSha1(context) }
                                    val currentPkg = context.packageName

                                    // Copyable details box
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("Package Name:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                                    Text(currentPkg, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = NeonCyan)
                                                }
                                                IconButton(
                                                    onClick = {
                                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                        clipboard.setPrimaryClip(ClipData.newPlainText("Package Name", currentPkg))
                                                        Toast.makeText(context, "Package Name copied!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.ContentCopy, "Copy", tint = NeonCyan, modifier = Modifier.size(16.dp))
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("SHA-1 Fingerprint:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                                    Text(currentSha1, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = NeonCyan, fontSize = 11.sp)
                                                }
                                                IconButton(
                                                    onClick = {
                                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                        clipboard.setPrimaryClip(ClipData.newPlainText("SHA-1 Fingerprint", currentSha1))
                                                        Toast.makeText(context, "SHA-1 Fingerprint copied!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.ContentCopy, "Copy", tint = NeonCyan, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Custom client ID configuration toggle
                        TextButton(
                            onClick = { showCustomOAuthConfig = !showCustomOAuthConfig },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Icon(Icons.Default.Key, null, modifier = Modifier.size(16.dp), tint = NeonCyan)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (showCustomOAuthConfig) "Hide OAuth Client ID Config" else "Enter Google Cloud OAuth Client ID",
                                style = MaterialTheme.typography.bodySmall,
                                color = NeonCyan
                            )
                        }

                        if (showCustomOAuthConfig) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkBackground),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = customIdInput,
                                        onValueChange = {
                                            customIdInput = it
                                            viewModel.updateCustomClientId(it)
                                        },
                                        label = { Text("Google Cloud OAuth Client ID") },
                                        placeholder = { Text("xxxx.apps.googleusercontent.com") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Redirect URI", "com.syncmyshit.app:/oauth2redirect"))
                                            Toast.makeText(context, "Redirect URI copied!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.fillMaxWidth().height(38.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(14.dp), tint = NeonCyan)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Copy Redirect URI (com.syncmyshit.app:/oauth2redirect)", fontSize = 11.sp, color = NeonCyan)
                                    }

                                    Text(
                                        text = "Setup in Google Cloud Console (100% free, 2 mins):\n" +
                                                "1. Go to console.cloud.google.com\n" +
                                                "2. Enable 'Google Drive API'\n" +
                                                "3. Under APIs & Services > Credentials > Create Credentials > OAuth client ID:\n" +
                                                "   • Choose 'Web application'\n" +
                                                "   • Under 'Authorized redirect URIs', paste the URI copied above\n" +
                                                "4. Copy the generated Client ID, paste it above, and tap 'Sign In via Web Browser'!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(onClick = { viewModel.prevStep() }) {
                            Text("Back", color = TextSecondary)
                        }

                        if (isConnected) {
                            Button(
                                onClick = { viewModel.nextStep() },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                            ) {
                                Text("Continue to Step 3", color = DarkSurface, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.nextStep() },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated)
                            ) {
                                Text("Skip for now →", color = TextPrimary)
                            }
                        }
                    }
                }
            }
        }

        // STEP 3: Auto-Discovery & Finish
        if (currentStep == 3) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Step 3: Save File Discovery",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    // ── Scanning in progress ──────────────────────────────────
                    if (isScanning) {
                        Text(
                            text = "Scanning all storage locations for emulators and save files…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = NeonCyan,
                            trackColor = DarkBackground
                        )
                        Text(
                            text = "This takes a moment on first run — checking internal storage, SD card, and all known emulator paths.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    } else {
                        // ── Scan done ─────────────────────────────────────────
                        val withSaves = discoveredList.filter { it.fileCount > 0 }
                        val detected  = discoveredList.filter { it.fileCount == 0 }
                        val saveTotal = discoveredList.sumOf { it.fileCount }

                        if (discoveredList.isEmpty()) {
                            // Nothing at all found
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkBackground),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "⚠️ No emulators or save files detected",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = StatusYellow
                                    )
                                    Text(
                                        text = "This usually means:\n" +
                                               "• Storage permission was not granted (go back to Step 1)\n" +
                                               "• You haven't played any games yet — that's OK! " +
                                               "Add paths manually after setup, or play a game first then rescan.\n" +
                                               "• Your device stores saves in a non-standard location — " +
                                               "use 'Add Custom Path' in the Emulators tab after setup.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Found ${discoveredList.size} system${if (discoveredList.size != 1) "s" else ""}" +
                                       if (saveTotal > 0) " with $saveTotal save files ready to sync!" else " — no saves yet (play a game first!).",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                // Show profiles with actual saves first
                                withSaves.take(6).forEach { profile ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(profile.name, color = TextPrimary, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                        Text("${profile.fileCount} saves", color = NeonCyan)
                                    }
                                }
                                // Then profiles detected (installed) but no saves yet
                                detected.take(4).forEach { profile ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(profile.name, color = TextMuted, modifier = Modifier.weight(1f))
                                        Text("detected, no saves yet", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                val more = discoveredList.size - withSaves.take(6).size - detected.take(4).size
                                if (more > 0) {
                                    Text("+ $more more…", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        // Rescan button — always available after first scan
                        OutlinedButton(
                            onClick = { viewModel.scanEmulators() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("🔄  Rescan Storage", color = NeonCyan)
                        }
                    }

                    // Finish button — always available so user isn't stuck
                    Button(
                        onClick = { viewModel.finishSetup(onSetupComplete) },
                        enabled = !isScanning,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Icon(Icons.Default.RocketLaunch, null, tint = DarkSurface)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Finish & Start Automagic Sync", color = DarkSurface, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StepIndicator(step: Int, currentStep: Int, label: String) {
    val isActive = currentStep == step
    val isDone = currentStep > step

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    if (isDone) StatusGreen
                    else if (isActive) NeonCyan
                    else DarkBorder
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isDone) {
                Icon(Icons.Default.CheckCircle, null, tint = DarkBackground, modifier = Modifier.size(16.dp))
            } else {
                Text(
                    text = "$step",
                    color = if (isActive) DarkBackground else TextSecondary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive || isDone) TextPrimary else TextMuted
        )
    }
}
