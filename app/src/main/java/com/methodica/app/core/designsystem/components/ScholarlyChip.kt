package com.methodica.app.core.designsystem.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

enum class AcademicChipStyle {
    Risk,
    Completed,
    Neutral
}

@Composable
fun ScholarlyAcademicChip(
    text: String,
    style: AcademicChipStyle,
    modifier: Modifier = Modifier
) {
    val colors = when (style) {
        AcademicChipStyle.Risk -> AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            labelColor = MaterialTheme.colorScheme.onErrorContainer
        )
        AcademicChipStyle.Completed -> AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            labelColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
        AcademicChipStyle.Neutral -> AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            labelColor = MaterialTheme.colorScheme.onSurface
        )
    }

    AssistChip(
        onClick = {},
        enabled = false,
        shape = RoundedCornerShape(50),
        label = { Text(text) },
        colors = colors,
        border = null,
        modifier = modifier
    )
}


