package com.syncmyshit.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.syncmyshit.app.data.model.EmulatorProfile
import com.syncmyshit.app.data.model.ProfileCategory
import com.syncmyshit.app.ui.theme.DarkBorder
import com.syncmyshit.app.ui.theme.DarkSurface
import com.syncmyshit.app.ui.theme.NeonCyan
import com.syncmyshit.app.ui.theme.TextPrimary
import com.syncmyshit.app.ui.theme.TextSecondary
import java.util.UUID

@Composable
fun CustomPathDialog(
    onDismiss: () -> Unit,
    onSave: (EmulatorProfile) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var system by remember { mutableStateOf("Custom Port") }
    var path by remember { mutableStateOf("") }
    var extensions by remember { mutableStateOf(".sav, .bin, .json") }
    var driveFolder by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text(
                text = "Add Custom Game / Save Path",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Add any standalone game, port, recomp, or custom emulator directory to automagically sync with Google Drive.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (driveFolder.isBlank()) {
                            driveFolder = it.replace(" ", "_")
                        }
                    },
                    label = { Text("Game or Emulator Name") },
                    placeholder = { Text("e.g. Zelda 64 Recomp") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = system,
                    onValueChange = { system = it },
                    label = { Text("Platform / Tag") },
                    placeholder = { Text("e.g. Recomp, Native Port, PS2") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = path,
                    onValueChange = { path = it },
                    label = { Text("Absolute Storage Directory Path") },
                    placeholder = { Text("/sdcard/GameSaves or /storage/emulated/0/...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = extensions,
                    onValueChange = { extensions = it },
                    label = { Text("File Extensions Filter (comma-separated)") },
                    placeholder = { Text(".sav, .bin, .dat, *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = driveFolder,
                    onValueChange = { driveFolder = it },
                    label = { Text("Google Drive Folder Name") },
                    placeholder = { Text("e.g. Recomp_Zelda") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Please enter a name"
                        return@Button
                    }
                    if (path.isBlank()) {
                        errorMessage = "Please enter a directory path"
                        return@Button
                    }

                    val extList = extensions.split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .map { if (it.startsWith(".") || it == "*") it else ".$it" }

                    val profile = EmulatorProfile(
                        id = "custom_${UUID.randomUUID()}",
                        name = name.trim(),
                        system = system.trim(),
                        category = ProfileCategory.CUSTOM,
                        candidatePaths = listOf(path.trim()),
                        resolvedSavePath = path.trim(),
                        fileExtensions = if (extList.isEmpty()) listOf("*") else extList,
                        driveSubfolder = if (driveFolder.isNotBlank()) driveFolder.trim() else name.replace(" ", "_"),
                        isEnabled = true,
                        isCustom = true
                    )
                    onSave(profile)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("Add & Track", color = DarkSurface)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
