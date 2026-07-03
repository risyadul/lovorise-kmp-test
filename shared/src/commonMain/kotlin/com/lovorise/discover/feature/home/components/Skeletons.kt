package com.lovorise.discover.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lovorise.discover.core.designsystem.LovoriseColors
import com.lovorise.discover.core.designsystem.components.shimmer

/** Shimmering placeholders shown while the mock API "loads". */
@Composable
fun StoriesRowSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        repeat(5) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .shimmer(),
                )
                Box(
                    Modifier
                        .width(44.dp)
                        .height(10.dp)
                        .clip(CircleShape)
                        .shimmer(),
                )
            }
        }
    }
}

@Composable
fun FeedCardSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, LovoriseColors.Border, RoundedCornerShape(24.dp))
            .background(Color.White),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .shimmer(),
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    Modifier
                        .width(130.dp)
                        .height(12.dp)
                        .clip(CircleShape)
                        .shimmer(),
                )
                Box(
                    Modifier
                        .width(90.dp)
                        .height(10.dp)
                        .clip(CircleShape)
                        .shimmer(),
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 5f)
                .shimmer(),
        )
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(CircleShape)
                    .shimmer(),
            )
            Box(
                Modifier
                    .width(180.dp)
                    .height(12.dp)
                    .clip(CircleShape)
                    .shimmer(),
            )
        }
    }
}
