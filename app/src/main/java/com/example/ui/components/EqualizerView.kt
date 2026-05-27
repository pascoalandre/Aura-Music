package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun EqualizerView(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 12,
    activeColor: Color = Color(0xFF1DB954), // Glow Spotify Green
    secondaryColor: Color = Color(0xFF00E676)
) {
    // Collect infinite animations for each bar to drive heights
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer")
    
    // Create an list of standard heights offset fractions to offset the sinewave
    val animFractions = (0 until barCount).map { index ->
        if (isPlaying) {
            infiniteTransition.animateFloat(
                initialValue = 0.15f,
                targetValue = 0.95f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 400 + (index * 80) % 350,
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$index"
            )
        } else {
            remember { mutableStateOf(0.08f) }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val spacing = 6f // Spacing between bars
        val totalSpace = spacing * (barCount - 1)
        val barWidth = (width - totalSpace) / barCount

        for (i in 0 until barCount) {
            val hFraction = animFractions[i].value
            val barHeight = height * hFraction
            val x = i * (barWidth + spacing)
            val y = height - barHeight

            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(secondaryColor, activeColor),
                    startY = y,
                    endY = height
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight)
            )
        }
    }
}
