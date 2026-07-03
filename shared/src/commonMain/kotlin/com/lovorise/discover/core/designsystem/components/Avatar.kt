package com.lovorise.discover.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import com.lovorise.discover.core.designsystem.LovoriseColors
import com.lovorise.discover.core.designsystem.poppinsFamily
import com.lovorise.discover.core.util.initialsOf

/**
 * Circular avatar that loads [imageUrl] and gracefully falls back to the
 * production-app style initials-on-muted-color disc while loading or offline.
 */
@Composable
fun Avatar(
    name: String,
    imageUrl: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    ringBrush: androidx.compose.ui.graphics.Brush? = null,
    ringWidth: Dp = 2.5.dp,
    ringGap: Dp = 2.5.dp,
) {
    val ringed = if (ringBrush != null) {
        modifier
            .size(size + (ringWidth + ringGap) * 2)
            .border(ringWidth, ringBrush, CircleShape)
            .padding(ringWidth + ringGap)
    } else {
        modifier.size(size)
    }

    Box(ringed.clip(CircleShape), contentAlignment = Alignment.Center) {
        val fallback: @Composable () -> Unit = {
            InitialsDisc(name = name)
        }
        if (imageUrl.isNullOrBlank()) {
            fallback()
        } else {
            SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = "$name avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { fallback() },
                error = { fallback() },
            )
        }
    }
}

@Composable
private fun InitialsDisc(name: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .background(LovoriseColors.avatarColor(name)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initialsOf(name),
            color = Color.White,
            fontFamily = poppinsFamily(),
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
        )
    }
}

/** Convenience for the brand story ring. */
@Composable
fun StoryRingAvatar(
    name: String,
    imageUrl: String?,
    size: Dp,
    seen: Boolean,
    modifier: Modifier = Modifier,
) {
    Avatar(
        name = name,
        imageUrl = imageUrl,
        size = size,
        modifier = modifier,
        ringBrush = if (seen) SolidColor(LovoriseColors.Border) else LovoriseColors.BrandGradient,
    )
}
