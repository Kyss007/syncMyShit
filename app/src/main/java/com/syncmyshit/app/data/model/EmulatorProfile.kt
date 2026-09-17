package com.syncmyshit.app.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class ProfileCategory {
    EMULATOR,
    RECOMP,
    STANDALONE_GAME,
    CUSTOM
}

@Serializable
data class EmulatorProfile(
    val id: String,
    val name: String,
    val system: String,
    val category: ProfileCategory = ProfileCategory.EMULATOR,
    val packageNames: List<String> = emptyList(),
    val candidatePaths: List<String> = emptyList(),
    val fileExtensions: List<String> = emptyList(),
    val driveSubfolder: String,
    val isEnabled: Boolean = true,
    val isCustom: Boolean = false,
    val lastSyncedTimestamp: Long = 0L,
    val resolvedSavePath: String? = null,
    val fileCount: Int = 0
)
