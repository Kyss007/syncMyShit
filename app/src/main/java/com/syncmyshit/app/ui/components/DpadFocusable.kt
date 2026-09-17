package com.syncmyshit.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.syncmyshit.app.ui.theme.DarkBorder
import com.syncmyshit.app.ui.theme.NeonCyan

@Composable
fun Modifier.dpadFocusable(
    shape: Shape = RoundedCornerShape(12.dp),
    focusedBorderWidth: Dp = 2.dp,
    unfocusedBorderWidth: Dp = 1.dp,
    focusedColor: Color = NeonCyan,
    unfocusedColor: Color = DarkBorder
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) focusedColor else unfocusedColor,
        animationSpec = tween(150),
        label = "dpadBorder"
    )

    return this
        .focusable(interactionSource = interactionSource)
        .border(
            width = if (isFocused) focusedBorderWidth else unfocusedBorderWidth,
            color = borderColor,
            shape = shape
        )
}
