package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AlbumArtView(
    path: String,
    modifier: Modifier = Modifier,
    fallbackTitle: String = "",
    fallbackArtist: String = ""
) {
    var albumArtBitmap by remember(path) { mutableStateOf<Bitmap?>(null) }
    var loaded by remember(path) { mutableStateOf(false) }

    LaunchedEffect(path) {
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(path)
                val artBytes = retriever.embeddedPicture
                if (artBytes != null) {
                    val bitmap = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
                    albumArtBitmap = bitmap
                }
            } catch (e: Exception) {
                // Return null on failure or if embedded art is absent
            } finally {
                try {
                    retriever.release()
                } catch (e: Exception) {
                    // Fail-safe release
                }
            }
            loaded = true
        }
    }

    if (albumArtBitmap != null) {
        Image(
            bitmap = albumArtBitmap!!.asImageBitmap(),
            contentDescription = "Arte de capa de $fallbackTitle",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        // Atmospheric stylized ambient gradient brush for fallback empty states
        val colors = remember(path) {
            val gradientPalettes = listOf(
                listOf(Color(0xFF00E676), Color(0xFF1DB954)),
                listOf(Color(0xFF2196F3), Color(0xFF0D47A1)),
                listOf(Color(0xFFE040FB), Color(0xFF6A1B9A)),
                listOf(Color(0xFFFF5252), Color(0xFFC62828)),
                listOf(Color(0xFFFFD700), Color(0xFFFF8C00))
            )
            // Stably choose a palette based on path string hash to keep it consistent
            val index = Math.abs(path.hashCode()) % gradientPalettes.size
            gradientPalettes[index]
        }

        Box(
            modifier = modifier
                .background(
                    Brush.radialGradient(
                        colors = colors,
                        center = Offset.Zero,
                        radius = 400f
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier.size(64.dp)
                )
                if (fallbackTitle.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = fallbackTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.Black.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
