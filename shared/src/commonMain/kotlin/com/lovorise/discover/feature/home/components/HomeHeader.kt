package com.lovorise.discover.feature.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lovorise.discover.core.designsystem.LovoriseColors
import com.lovorise.discover.feature.home.HomeTab

/**
 * Discover header: For You / Connections segmented pills on the left,
 * Search and Notifications (UI-only, badged) actions on the right.
 */
@Composable
fun HomeHeader(
    tab: HomeTab,
    notificationCount: Int,
    onTabSelected: (HomeTab) -> Unit,
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderTabPill(
            text = "For You",
            selected = tab == HomeTab.FOR_YOU,
            onClick = { onTabSelected(HomeTab.FOR_YOU) },
        )
        HeaderTabPill(
            text = "Connections",
            selected = tab == HomeTab.CONNECTIONS,
            onClick = { onTabSelected(HomeTab.CONNECTIONS) },
        )

        Box(Modifier.weight(1f))

        IconButton(onClick = onSearchClick) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Search",
                tint = LovoriseColors.Ink,
                modifier = Modifier.size(24.dp),
            )
        }
        Box {
            IconButton(onClick = onNotificationsClick) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    tint = LovoriseColors.Ink,
                    modifier = Modifier.size(24.dp),
                )
            }
            if (notificationCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-6).dp, y = 6.dp)
                        .size(15.dp)
                        .clip(CircleShape)
                        .background(LovoriseColors.Pink),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = notificationCount.coerceAtMost(9).toString(),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        lineHeight = 9.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderTabPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val background by animateColorAsState(
        if (selected) LovoriseColors.Ink else Color.Transparent,
        label = "tabBg",
    )
    val contentColor by animateColorAsState(
        if (selected) Color.White else LovoriseColors.Muted,
        label = "tabText",
    )
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                role = Role.Tab,
                onClick = onClick,
            )
            .height(38.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = contentColor,
        )
    }
}
