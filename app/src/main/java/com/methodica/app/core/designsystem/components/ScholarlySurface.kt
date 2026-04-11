package com.methodica.app.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

enum class ScholarlySurfaceTone {
    Base,
    Content,
    Focus
}

@Composable
fun ScholarlySectionSurface(
    modifier: Modifier = Modifier,
    tone: ScholarlySurfaceTone = ScholarlySurfaceTone.Content,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit
) {
    val containerColor = when (tone) {
        ScholarlySurfaceTone.Base -> MaterialTheme.colorScheme.surface
        ScholarlySurfaceTone.Content -> MaterialTheme.colorScheme.surfaceContainer
        ScholarlySurfaceTone.Focus -> MaterialTheme.colorScheme.surfaceContainerHighest
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .padding(contentPadding)
    ) {
        content()
    }
}

