package com.syncmyshit.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.syncmyshit.app.data.model.FileSyncState
import com.syncmyshit.app.ui.theme.DarkBackground
import com.syncmyshit.app.ui.theme.StatusGreen
import com.syncmyshit.app.ui.theme.StatusRed
import com.syncmyshit.app.ui.theme.StatusYellow

@Composable
fun StatusBadge(
    syncState: FileSyncState,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when (syncState) {
        FileSyncState.SYNCED -> Triple(StatusGreen.copy(alpha = 0.2f), StatusGreen, "SYNCED")
        FileSyncState.LOCAL_NEWER -> Triple(StatusYellow.copy(alpha = 0.2f), StatusYellow, "LOCAL NEWER")
        FileSyncState.CLOUD_NEWER -> Triple(StatusYellow.copy(alpha = 0.2f), StatusYellow, "CLOUD NEWER")
        FileSyncState.CONFLICT -> Triple(StatusRed.copy(alpha = 0.2f), StatusRed, "CONFLICT")
        FileSyncState.LOCAL_ONLY -> Triple(Color.White.copy(alpha = 0.1f), Color.White, "LOCAL ONLY")
        FileSyncState.CLOUD_ONLY -> Triple(Color.White.copy(alpha = 0.1f), Color.White, "CLOUD ONLY")
        FileSyncState.ERROR -> Triple(StatusRed.copy(alpha = 0.2f), StatusRed, "ERROR")
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
