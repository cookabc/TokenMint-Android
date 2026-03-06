package com.chuangcius.tokenmint.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chuangcius.tokenmint.ui.theme.TokenMintCountdown
import com.chuangcius.tokenmint.ui.theme.TokenMintCountdownUrgent

/**
 * Circular countdown ring showing remaining seconds in the TOTP period.
 * Color changes to urgent red when ≤5 seconds remain.
 */
@Composable
fun CountdownRing(
    progress: Float,
    remaining: Int,
    modifier: Modifier = Modifier
) {
    val ringColor = if (remaining <= 5) TokenMintCountdownUrgent else TokenMintCountdown
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val textColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier.size(32.dp)) {
        val strokeWidth = 3.dp.toPx()
        val diameter = size.minDimension
        val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
        val arcSize = Size(diameter - strokeWidth, diameter - strokeWidth)

        // Track
        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth)
        )

        // Progress arc
        drawArc(
            color = ringColor,
            startAngle = -90f,
            sweepAngle = -360f * (1f - progress),
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Center text
        val text = remaining.toString()
        val textStyle = TextStyle(
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Default,
            color = textColor
        )
        val textLayoutResult = textMeasurer.measure(text, textStyle)
        drawText(
            textLayoutResult = textLayoutResult,
            topLeft = Offset(
                (size.width - textLayoutResult.size.width) / 2,
                (size.height - textLayoutResult.size.height) / 2
            )
        )
    }
}
