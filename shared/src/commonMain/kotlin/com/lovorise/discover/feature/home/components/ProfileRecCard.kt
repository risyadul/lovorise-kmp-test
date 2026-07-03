package com.lovorise.discover.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.lovorise.discover.core.designsystem.LovoriseColors
import com.lovorise.discover.core.designsystem.components.LovoriseIconButton
import com.lovorise.discover.core.designsystem.components.LovorisePillButton
import com.lovorise.discover.core.designsystem.components.shimmer
import com.lovorise.discover.core.util.formatDistance
import com.lovorise.discover.core.util.initialsOf
import com.lovorise.discover.data.model.FeedItem
import com.lovorise.discover.data.model.ScoredProfile
import com.lovorise.discover.data.model.UserProfile

/**
 * "People you may connect with" module woven into the feed: a horizontally
 * scrolling set of recommended profiles with match-reason chips.
 */
@Composable
fun ProfileRecsSection(
    item: FeedItem.ProfileRecs,
    requestedConnections: Set<String>,
    sharedInterestsOf: (UserProfile) -> List<String>,
    onConnect: (UserProfile) -> Unit,
    onMessage: (UserProfile) -> Unit,
    onShareProfile: (UserProfile) -> Unit,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "People you may connect with",
                style = MaterialTheme.typography.titleMedium,
                color = LovoriseColors.Ink,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "See all",
                style = MaterialTheme.typography.labelLarge,
                color = LovoriseColors.Pink,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onSeeAll)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(item.profiles, key = { it.user.id }) { scored ->
                ProfileRecCard(
                    scored = scored,
                    requested = scored.user.id in requestedConnections,
                    sharedInterests = sharedInterestsOf(scored.user),
                    onConnect = { onConnect(scored.user) },
                    onMessage = { onMessage(scored.user) },
                    onShareProfile = { onShareProfile(scored.user) },
                )
            }
        }
    }
}

@Composable
private fun ProfileRecCard(
    scored: ScoredProfile,
    requested: Boolean,
    sharedInterests: List<String>,
    onConnect: () -> Unit,
    onMessage: () -> Unit,
    onShareProfile: () -> Unit,
) {
    val user = scored.user
    Column(
        modifier = Modifier
            .width(236.dp)
            .clip(RoundedCornerShape(22.dp))
            .border(1.dp, LovoriseColors.Border, RoundedCornerShape(22.dp))
            .background(Color.White),
    ) {
        // Portrait -----------------------------------------------------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .background(LovoriseColors.SurfaceDim),
        ) {
            SubcomposeAsyncImage(
                model = user.photoUrl,
                contentDescription = "${user.name} photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { Box(Modifier.fillMaxSize().shimmer()) },
                error = {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(LovoriseColors.avatarColor(user.name)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = initialsOf(user.name),
                            style = MaterialTheme.typography.displaySmall,
                            color = Color.White.copy(alpha = 0.85f),
                        )
                    }
                },
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, LovoriseColors.MediaScrim),
                        ),
                    ),
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${user.name.substringBefore(' ')}, ${user.age}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (user.isVerified) {
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = "Verified",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(start = 5.dp)
                            .size(15.dp),
                    )
                }
                if (user.lastActiveMinutes <= 20) {
                    Box(
                        Modifier
                            .padding(start = 7.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(LovoriseColors.Success),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.9f))
                    .padding(horizontal = 9.dp, vertical = 4.dp),
            ) {
                Text(
                    text = formatDistance(user.distanceKm),
                    style = MaterialTheme.typography.labelSmall,
                    color = LovoriseColors.Ink,
                )
            }
        }

        // Details --------------------------------------------------------------
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (scored.reasons.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    scored.reasons.take(2).forEach { reason ->
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(LovoriseColors.PinkSoft)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Text(
                                text = reason,
                                style = MaterialTheme.typography.labelSmall,
                                color = LovoriseColors.Pink,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
            Text(
                text = user.bio,
                style = MaterialTheme.typography.bodySmall,
                color = LovoriseColors.Slate,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.height(34.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LovorisePillButton(
                    text = if (requested) "Requested" else "Connect",
                    icon = if (requested) Icons.Outlined.Check else null,
                    filled = !requested,
                    compact = true,
                    onClick = onConnect,
                    modifier = Modifier.weight(1f),
                )
                LovoriseIconButton(
                    icon = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = "Send message request",
                    size = 34.dp,
                    onClick = onMessage,
                )
                LovoriseIconButton(
                    icon = Icons.Outlined.Share,
                    contentDescription = "Share profile",
                    size = 34.dp,
                    onClick = onShareProfile,
                )
            }
        }
    }
}

