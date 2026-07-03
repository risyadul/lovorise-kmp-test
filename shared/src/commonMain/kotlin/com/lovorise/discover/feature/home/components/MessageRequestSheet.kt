package com.lovorise.discover.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lovorise.discover.core.designsystem.LovoriseColors
import com.lovorise.discover.core.designsystem.components.Avatar
import com.lovorise.discover.core.designsystem.components.LovorisePillButton
import com.lovorise.discover.data.model.UserProfile

private val QUICK_MESSAGES = listOf(
    "Hey! Love your feed ✨",
    "Coffee sometime? ☕",
    "That last post is so cool!",
)

/** Bottom sheet for composing a message request to [user]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageRequestSheet(
    user: UserProfile,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit,
) {
    var draft by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Avatar(name = user.name, imageUrl = user.photoUrl, size = 46.dp)
                Column {
                    Text(
                        text = "Message request to ${user.name.substringBefore(' ')}",
                        style = MaterialTheme.typography.titleMedium,
                        color = LovoriseColors.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "They'll see your request in their inbox",
                        style = MaterialTheme.typography.bodySmall,
                        color = LovoriseColors.Muted,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QUICK_MESSAGES.forEach { quick ->
                    Text(
                        text = quick,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (draft == quick) Color.White else LovoriseColors.Slate,
                        maxLines = 1,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clip(CircleShape)
                            .background(if (draft == quick) LovoriseColors.Pink else LovoriseColors.SurfaceDim)
                            .clickable { draft = quick }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = {
                    Text(
                        "Write something nice…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LovoriseColors.Muted,
                    )
                },
                textStyle = MaterialTheme.typography.bodyMedium,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LovoriseColors.Pink,
                    unfocusedBorderColor = LovoriseColors.Border,
                    cursorColor = LovoriseColors.Pink,
                ),
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            LovorisePillButton(
                text = "Send request",
                onClick = { if (draft.isNotBlank()) onSend(draft) },
                filled = draft.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
