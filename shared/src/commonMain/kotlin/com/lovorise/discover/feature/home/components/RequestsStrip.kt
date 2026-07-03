package com.lovorise.discover.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lovorise.discover.core.designsystem.LovoriseColors
import com.lovorise.discover.core.designsystem.components.Avatar
import com.lovorise.discover.core.designsystem.components.LovoriseIconButton
import com.lovorise.discover.core.designsystem.components.LovorisePillButton
import com.lovorise.discover.core.util.formatDistance
import com.lovorise.discover.data.model.UserProfile

/** Pending connection requests, shown at the top of the Connections tab. */
@Composable
fun RequestsStrip(
    requests: List<UserProfile>,
    onAccept: (UserProfile) -> Unit,
    onDecline: (UserProfile) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .border(1.dp, LovoriseColors.Border, RoundedCornerShape(22.dp))
            .background(Color.White)
            .padding(vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Connection requests",
                style = MaterialTheme.typography.titleMedium,
                color = LovoriseColors.Ink,
            )
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(LovoriseColors.PinkSoft)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    text = requests.size.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = LovoriseColors.Pink,
                )
            }
        }
        requests.forEach { user ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Avatar(name = user.name, imageUrl = user.photoUrl, size = 46.dp)
                Column(Modifier.weight(1f)) {
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = LovoriseColors.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${formatDistance(user.distanceKm)} • ${user.interests.take(2).joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = LovoriseColors.Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                LovorisePillButton(
                    text = "Accept",
                    compact = true,
                    onClick = { onAccept(user) },
                )
                LovoriseIconButton(
                    icon = Icons.Outlined.Close,
                    contentDescription = "Decline request",
                    size = 34.dp,
                    tint = LovoriseColors.Muted,
                    onClick = { onDecline(user) },
                )
            }
        }
    }
}
