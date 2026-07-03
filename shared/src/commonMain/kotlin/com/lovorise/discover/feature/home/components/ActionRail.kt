package com.lovorise.discover.feature.home.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.lovorise.discover.core.designsystem.LovoriseColors
import com.lovorise.discover.core.util.formatCount
import androidx.compose.animation.core.Animatable

private val RailScrim = Color(0x52101828)

/**
 * Vertical action sidebar overlaid on feed media: React, Message request,
 * Share and Save — each with a live engagement counter.
 */
@Composable
fun ActionRail(
    reacted: Boolean,
    reactionCount: Int,
    saved: Boolean,
    saveCount: Int,
    shareCount: Int,
    onReact: () -> Unit,
    onMessage: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(CircleShape)
            .background(RailScrim)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        RailAction(
            icon = if (reacted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            tint = if (reacted) LovoriseColors.Pink else Color.White,
            label = formatCount(reactionCount),
            contentDescription = if (reacted) "Remove reaction" else "React",
            animateKey = reacted,
            onClick = onReact,
        )
        RailAction(
            icon = Icons.Outlined.ChatBubbleOutline,
            tint = Color.White,
            label = "Chat",
            contentDescription = "Send message request",
            onClick = onMessage,
        )
        RailAction(
            icon = Icons.Outlined.Share,
            tint = Color.White,
            label = formatCount(shareCount),
            contentDescription = "Share",
            onClick = onShare,
        )
        RailAction(
            icon = if (saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
            tint = if (saved) LovoriseColors.Pink else Color.White,
            label = formatCount(saveCount),
            contentDescription = if (saved) "Remove from saved" else "Save",
            animateKey = saved,
            onClick = onSave,
        )
    }
}

@Composable
private fun RailAction(
    icon: ImageVector,
    tint: Color,
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
    animateKey: Boolean? = null,
) {
    val scale = remember { Animatable(1f) }
    if (animateKey != null) {
        val previous = remember { arrayOf<Boolean?>(null) }
        LaunchedEffect(animateKey) {
            if (previous[0] != null && previous[0] != animateKey) {
                scale.snapTo(0.65f)
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMedium),
                )
            }
            previous[0] = animateKey
        }
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Color.White, bounded = false),
                onClick = onClick,
            )
            .padding(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier
                .size(24.dp)
                .scale(scale.value),
        )
        AnimatedContent(
            targetState = label,
            transitionSpec = {
                (slideInVertically { it / 2 } + fadeIn()) togetherWith
                    (slideOutVertically { -it / 2 } + fadeOut())
            },
            label = "railCounter",
        ) { value ->
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
        }
    }
}
