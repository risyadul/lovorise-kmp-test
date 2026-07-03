package com.lovorise.discover.feature.home.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.lovorise.discover.core.designsystem.LovoriseColors
import com.lovorise.discover.core.designsystem.components.Avatar
import com.lovorise.discover.core.designsystem.components.shimmer
import com.lovorise.discover.core.util.timeAgo
import com.lovorise.discover.data.model.FeedItem

/**
 * Content card for the Discover feed: author header, 4:5 media with the
 * action sidebar overlay, "why you see this" chip, and an expandable caption.
 */
@Composable
fun FeedPostCard(
    item: FeedItem.Post,
    reacted: Boolean,
    saved: Boolean,
    sharedLocally: Boolean,
    hasStory: Boolean,
    storySeen: Boolean,
    onAuthorClick: () -> Unit,
    onReact: () -> Unit,
    onDoubleTapReact: () -> Unit,
    onMessage: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val post = item.post
    val author = item.author

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, LovoriseColors.Border, RoundedCornerShape(24.dp))
            .background(Color.White),
    ) {
        // Author header -----------------------------------------------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val ring = when {
                !hasStory -> null
                storySeen -> SolidColor(LovoriseColors.Border)
                else -> LovoriseColors.BrandGradient
            }
            Avatar(
                name = author.name,
                imageUrl = author.photoUrl,
                size = 40.dp,
                ringBrush = ring,
                ringWidth = 2.dp,
                ringGap = 2.dp,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false),
                    onClick = onAuthorClick,
                ),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = author.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = LovoriseColors.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (author.isVerified) {
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = "Verified",
                            tint = LovoriseColors.Pink,
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .size(14.dp),
                        )
                    }
                }
                Text(
                    text = listOfNotNull(post.location, timeAgo(post.postedMinutesAgo)).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = LovoriseColors.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onMore) {
                Icon(
                    imageVector = Icons.Outlined.MoreHoriz,
                    contentDescription = "More options",
                    tint = LovoriseColors.Muted,
                )
            }
        }

        // Media + overlays ----------------------------------------------------
        var burstCount by remember(post.id) { mutableIntStateOf(0) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 5f)
                .background(LovoriseColors.SurfaceDim)
                .pointerInput(post.id) {
                    detectTapGestures(
                        onDoubleTap = {
                            onDoubleTapReact()
                            burstCount++
                        },
                    )
                },
        ) {
            SubcomposeAsyncImage(
                model = post.imageUrl,
                contentDescription = post.caption,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { Box(Modifier.fillMaxSize().shimmer()) },
                error = {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(LovoriseColors.avatarColor(post.id)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(56.dp),
                        )
                    }
                },
            )

            if (item.reason != null) {
                ReasonChip(
                    reason = item.reason,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                )
            }

            ActionRail(
                reacted = reacted,
                reactionCount = post.reactions + if (reacted) 1 else 0,
                saved = saved,
                saveCount = post.saves + if (saved) 1 else 0,
                shareCount = post.shares + if (sharedLocally) 1 else 0,
                onReact = onReact,
                onMessage = onMessage,
                onShare = onShare,
                onSave = onSave,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
            )

            HeartBurst(
                trigger = burstCount,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // Caption ------------------------------------------------------------
        var expanded by remember(post.id) { mutableStateOf(false) }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = post.caption,
                style = MaterialTheme.typography.bodyMedium,
                color = LovoriseColors.Slate,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (post.tags.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    post.tags.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(LovoriseColors.SurfaceDim)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "#${tag.replace(" ", "")}",
                                style = MaterialTheme.typography.labelMedium,
                                color = LovoriseColors.Slate,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReasonChip(reason: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.92f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.AutoAwesome,
            contentDescription = null,
            tint = LovoriseColors.Pink,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = reason,
            style = MaterialTheme.typography.labelMedium,
            color = LovoriseColors.Pink,
        )
    }
}

/** Instagram-style heart pop shown on double tap. */
@Composable
private fun HeartBurst(trigger: Int, modifier: Modifier = Modifier) {
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        alpha.snapTo(1f)
        scale.snapTo(0.4f)
        scale.animateTo(1.15f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium))
        alpha.animateTo(0f, tween(durationMillis = 320, delayMillis = 160))
        scale.snapTo(0f)
    }
    Icon(
        imageVector = Icons.Filled.Favorite,
        contentDescription = null,
        tint = Color.White,
        modifier = modifier
            .size(96.dp)
            .scale(scale.value)
            .alpha(alpha.value),
    )
}
