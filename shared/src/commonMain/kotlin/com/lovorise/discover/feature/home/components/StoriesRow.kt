package com.lovorise.discover.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lovorise.discover.core.designsystem.LovoriseColors
import com.lovorise.discover.core.designsystem.components.Avatar
import com.lovorise.discover.core.designsystem.components.StoryRingAvatar
import com.lovorise.discover.data.model.StoryUi

/**
 * Horizontal stories feed. Unseen stories carry the warm brand ring, stories
 * with several photos show a count badge, and "Your story" leads the row.
 */
@Composable
fun StoriesRow(
    stories: List<StoryUi>,
    viewerName: String,
    viewerPhotoUrl: String?,
    onStoryClick: (StoryUi) -> Unit,
    onAddStoryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "your-story") {
            YourStoryBubble(
                name = viewerName,
                photoUrl = viewerPhotoUrl,
                onClick = onAddStoryClick,
            )
        }
        items(stories, key = { it.story.id }) { storyUi ->
            StoryBubble(storyUi = storyUi, onClick = { onStoryClick(storyUi) })
        }
    }
}

@Composable
private fun YourStoryBubble(
    name: String,
    photoUrl: String?,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick,
            )
            .padding(2.dp),
    ) {
        Box {
            Avatar(
                name = name,
                imageUrl = photoUrl,
                size = 62.dp,
                modifier = Modifier.border(1.5.dp, LovoriseColors.Border, CircleShape).padding(4.dp),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(LovoriseColors.Pink),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Add story",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Text(
            text = "Your story",
            style = MaterialTheme.typography.labelMedium,
            color = LovoriseColors.Slate,
            maxLines = 1,
        )
    }
}

@Composable
private fun StoryBubble(
    storyUi: StoryUi,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick,
            )
            .padding(2.dp),
    ) {
        Box {
            StoryRingAvatar(
                name = storyUi.user.name,
                imageUrl = storyUi.user.photoUrl,
                size = 60.dp,
                seen = storyUi.seen,
            )
            val imageCount = storyUi.story.images.size
            if (imageCount > 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(Color.White)
                        .padding(1.5.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(LovoriseColors.Ink)
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = imageCount.toString(),
                        color = Color.White,
                        fontSize = 9.sp,
                        lineHeight = 12.sp,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
        Text(
            text = storyUi.user.name.substringBefore(' '),
            style = MaterialTheme.typography.labelMedium,
            color = if (storyUi.seen) LovoriseColors.Muted else LovoriseColors.Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(66.dp),
            textAlign = TextAlign.Center,
        )
    }
}
