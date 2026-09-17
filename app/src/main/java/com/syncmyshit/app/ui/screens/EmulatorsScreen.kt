package com.syncmyshit.app.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.syncmyshit.app.data.model.ProfileCategory
import com.syncmyshit.app.ui.components.SyncItemCard
import com.syncmyshit.app.ui.components.dpadFocusable
import com.syncmyshit.app.ui.theme.DarkBackground
import com.syncmyshit.app.ui.theme.DarkSurface
import com.syncmyshit.app.ui.theme.NeonCyan
import com.syncmyshit.app.ui.theme.NeonPurple
import com.syncmyshit.app.ui.theme.TextMuted
import com.syncmyshit.app.ui.theme.TextPrimary
import com.syncmyshit.app.ui.theme.TextSecondary
import com.syncmyshit.app.ui.viewmodel.MainViewModel

@Composable
fun EmulatorsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val emulators by viewModel.emulators.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    var selectedFilter by remember { mutableStateOf<ProfileCategory?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredList = remember(emulators, selectedFilter) {
        if (selectedFilter == null) emulators
        else emulators.filter { it.category == selectedFilter }
    }

    if (showAddDialog) {
        CustomPathDialog(
            onDismiss = { showAddDialog = false },
            onSave = { profile ->
                viewModel.addCustomProfile(profile)
                showAddDialog = false
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Emulators & Games",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Text(
                    text = "${emulators.size} items detected on this device",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { viewModel.refreshEmulators() },
                    modifier = Modifier.dpadFocusable(shape = RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Rescan Storage",
                        tint = NeonCyan
                    )
                }

                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                    modifier = Modifier.dpadFocusable(shape = RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Custom")
                }
            }
        }

        // Scanning progress indicator — visible on slow devices (GammaOS, de-Googled ROMs)
        if (isScanning) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Scanning storage for emulators & save files…",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeonCyan
                )
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = NeonCyan,
                    trackColor = DarkSurface
                )
            }
        }

        // Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == null,
                onClick = { selectedFilter = null },
                label = { Text("All (${emulators.size})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                    selectedLabelColor = NeonCyan
                )
            )

            FilterChip(
                selected = selectedFilter == ProfileCategory.EMULATOR,
                onClick = { selectedFilter = ProfileCategory.EMULATOR },
                label = { Text("Emulators") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                    selectedLabelColor = NeonCyan
                )
            )

            FilterChip(
                selected = selectedFilter == ProfileCategory.RECOMP,
                onClick = { selectedFilter = ProfileCategory.RECOMP },
                label = { Text("Recomps") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                    selectedLabelColor = NeonCyan
                )
            )

            FilterChip(
                selected = selectedFilter == ProfileCategory.CUSTOM,
                onClick = { selectedFilter = ProfileCategory.CUSTOM },
                label = { Text("Custom") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                    selectedLabelColor = NeonCyan
                )
            )
        }

        // List
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isScanning) {
                        Text(
                            text = "Scanning storage…",
                            style = MaterialTheme.typography.bodyLarge,
                            color = NeonCyan
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "This may take a moment on first run.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    } else {
                        Text(
                            text = "No save files found on this device.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Make sure Storage permission is granted,\nor add a custom save path manually.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = { showAddDialog = true }) {
                            Text("Add a Save Path Manually", color = NeonCyan)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { it.id }) { profile ->
                    SyncItemCard(
                        profile = profile,
                        onToggle = { enabled -> viewModel.toggleProfile(profile.id, enabled) },
                        onSyncNow = { viewModel.syncProfile(profile) }
                    )
                }
            }
        }
    }
}
