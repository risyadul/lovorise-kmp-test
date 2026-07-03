package com.lovorise.discover.feature.story

import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.SubcomposeAsyncImage
import com.lovorise.discover.core.designsystem.LovoriseColors
import com.lovorise.discover.core.designsystem.components.Avatar
import com.lovorise.discover.core.designsystem.components.shimmer
import com.lovorise.discover.core.platform.rememberShareText
import com.lovorise.discover.core.util.timeAgo
import com.lovorise.discover.data.model.StoryUi
import kotlinx.coroutines.launch

private const val IMAGE_DURATION_MS = 4_500

private val QUICK_REACTIONS = listOf("❤️", "🔥", "😂", "😍", "👏")

/**
 * Full-screen story experience: segmented auto-advance progress, tap to
 * navigate, hold to pause, swipe down to dismiss, "2/6" pagination for
 * multi-image stories, and React / Connect / Share / Message / Save actions.
 */
@Composable
fun StoryViewerScreen(
    startStoryId: String,
    onClose: () -> Unit,
    viewModel: StoryViewerViewModel = viewModel(factory = StoryViewerViewModel.Factory),
) {
    val stories = viewModel.stories
    if (!viewModel.isLoaded) {
        Box(Modifier.fillMaxSize().background(Color.Black))
        return
    }
    if (stories.isEmpty()) {
        LaunchedEffect(Unit) { onClose() }
        return
    }

    val startIndex = remember(startStoryId) {
        stories.indexOfFirst { it.story.id == startStoryId }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(initialPage = startIndex) { stories.size }
    val pagerScope = rememberCoroutineScope()

    BackHandler(onBack = onClose)

    // Swipe-down-to-dismiss offset.
    var dragOffset by remember { mutableStateOf(0f) }
    val dismissThresholdPx = with(LocalDensity.current) { 140.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        dragOffset = (dragOffset + dragAmount).coerceAtLeast(0f)
                    },
                    onDragEnd = {
                        if (dragOffset > dismissThresholdPx) onClose() else dragOffset = 0f
                    },
                    onDragCancel = { dragOffset = 0f },
                )
            }
            .graphicsLayer {
                translationY = dragOffset
                val progress = (dragOffset / (dismissThresholdPx * 3)).coerceIn(0f, 0.12f)
                scaleX = 1f - progress
                scaleY = 1f - progress
            },
    ) {
        HorizontalPager(state = pagerState) { page ->
            val storyUi = stories[page]
            StoryPage(
                storyUi = storyUi,
                isActive = pagerState.currentPage == page && dragOffset == 0f,
                viewModel = viewModel,
                onFinished = {
                    if (page < stories.lastIndex) {
                        pagerScope.launch { pagerState.animateScrollToPage(page + 1) }
                    } else {
                        onClose()
                    }
                },
                onPrevStory = {
                    if (page > 0) {
                        pagerScope.launch { pagerState.animateScrollToPage(page - 1) }
                    }
                },
                onClose = onClose,
            )
        }
    }
}

@Composable
private fun StoryPage(
    storyUi: StoryUi,
    isActive: Boolean,
    viewModel: StoryViewerViewModel,
    onFinished: () -> Unit,
    onPrevStory: () -> Unit,
    onClose: () -> Unit,
) {
    val story = storyUi.story
    val user = storyUi.user
    val shareText = rememberShareText()
    val scope = rememberCoroutineScope()

    var imageIndex by remember(story.id) { mutableIntStateOf(0) }
    var paused by remember(story.id) { mutableStateOf(false) }
    var replyDraft by remember(story.id) { mutableStateOf("") }
    val progress = remember(story.id, imageIndex) { Animatable(0f) }

    val session by viewModel.session.collectAsState()
    val requested = user.id in session.requestedConnections
    val saved = story.id in session.savedStories
    val reactedEmoji = session.storyReactions[story.id]

    // Floating reaction animation state.
    var burstEmoji by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(story.id, isActive) {
        if (isActive) viewModel.markSeen(story.id)
    }

    // Auto-advance timer driving the segmented progress bar.
    LaunchedEffect(story.id, imageIndex, isActive, paused) {
        if (!isActive || paused) return@LaunchedEffect
        val remaining = ((1f - progress.value) * IMAGE_DURATION_MS).toInt().coerceAtLeast(1)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = remaining, easing = LinearEasing),
        )
        if (imageIndex < story.images.lastIndex) {
            imageIndex++
        } else {
            onFinished()
        }
    }

    fun goNextImage() {
        if (imageIndex < story.images.lastIndex) {
            imageIndex++
        } else {
            onFinished()
        }
    }

    fun goPrevImage() {
        when {
            progress.value > 0.25f -> scope.launch { progress.snapTo(0f) }
            imageIndex > 0 -> imageIndex--
            else -> onPrevStory()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // Media with tap zones + hold to pause -------------------------------
        SubcomposeAsyncImage(
            model = story.images[imageIndex],
            contentDescription = "${user.name}'s story",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(story.id, imageIndex) {
                    detectTapGestures(
                        onTap = { offset ->
                            if (offset.x < size.width * 0.35f) goPrevImage() else goNextImage()
                        },
                        // Present so a hold-to-pause release counts as a long
                        // press, not a tap — otherwise letting go would
                        // navigate the story. Pausing itself happens in onPress.
                        onLongPress = {},
                        onPress = {
                            paused = true
                            try {
                                awaitRelease()
                            } finally {
                                paused = false
                            }
                        },
                    )
                },
            loading = { Box(Modifier.fillMaxSize().shimmer()) },
            error = {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(LovoriseColors.avatarColor(story.id)),
                )
            },
        )

        // Top and bottom scrims for legibility --------------------------------
        Box(
            Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent),
                    ),
                ),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(170.dp)
                .align(Alignment.BottomCenter)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                    ),
                ),
        )

        // Header: progress + author + pagination + close ----------------------
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                story.images.forEachIndexed { index, _ ->
                    val fill = when {
                        index < imageIndex -> 1f
                        index == imageIndex -> progress.value
                        else -> 0f
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.35f)),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(fill)
                                .height(3.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Avatar(name = user.name, imageUrl = user.photoUrl, size = 38.dp)
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user.name,
                            style = MaterialTheme.typography.titleSmall,
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
                                    .padding(start = 4.dp)
                                    .size(13.dp),
                            )
                        }
                    }
                    Text(
                        text = timeAgo(story.postedMinutesAgo),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }
                if (story.images.size > 1) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "${imageIndex + 1}/${story.images.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                        )
                    }
                }
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close story",
                        tint = Color.White,
                    )
                }
            }
        }

        // Right action rail ----------------------------------------------------
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            StoryAction(
                icon = if (reactedEmoji != null) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                tint = if (reactedEmoji != null) LovoriseColors.Pink else Color.White,
                label = "React",
                onClick = {
                    viewModel.react(story.id, "❤️")
                    burstEmoji = "❤️"
                },
            )
            StoryAction(
                icon = Icons.Outlined.PersonAdd,
                tint = if (requested) LovoriseColors.Pink else Color.White,
                label = if (requested) "Sent" else "Connect",
                onClick = { viewModel.requestConnection(user) },
            )
            StoryAction(
                icon = Icons.Outlined.Share,
                tint = Color.White,
                label = "Share",
                onClick = {
                    shareText("Check out ${user.name}'s story on Lovorise 💗")
                },
            )
            StoryAction(
                icon = if (saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                tint = if (saved) LovoriseColors.Pink else Color.White,
                label = if (saved) "Saved" else "Save",
                onClick = { viewModel.toggleSave(story.id) },
            )
        }

        // Bottom: quick reactions + reply field --------------------------------
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(0.8f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                QUICK_REACTIONS.forEach { emoji ->
                    Text(
                        text = emoji,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (reactedEmoji == emoji) Color.White.copy(alpha = 0.28f)
                                else Color.Transparent,
                            )
                            .clickable {
                                viewModel.react(story.id, emoji)
                                burstEmoji = emoji
                            }
                            .padding(4.dp),
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f))
                        .padding(horizontal = 16.dp, vertical = 11.dp),
                ) {
                    if (replyDraft.isEmpty()) {
                        Text(
                            text = "Send a message request…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.75f),
                        )
                    }
                    BasicTextField(
                        value = replyDraft,
                        onValueChange = { replyDraft = it },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (replyDraft.isNotBlank()) {
                                    viewModel.sendMessageRequest(user, replyDraft)
                                    replyDraft = ""
                                }
                            },
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                IconButton(
                    onClick = {
                        if (replyDraft.isNotBlank()) {
                            viewModel.sendMessageRequest(user, replyDraft)
                            replyDraft = ""
                        }
                    },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send message request",
                        tint = if (replyDraft.isBlank()) Color.White.copy(alpha = 0.5f) else LovoriseColors.Pink,
                    )
                }
            }
        }

        // Center emoji burst ----------------------------------------------------
        EmojiBurst(
            emoji = burstEmoji,
            onDone = { burstEmoji = null },
            modifier = Modifier.align(Alignment.Center),
        )

        // Toast-like confirmation from the view model ---------------------------
        viewModel.transientMessage?.let { message ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 92.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                )
            }
            LaunchedEffect(message) {
                kotlinx.coroutines.delay(1_800)
                viewModel.clearTransientMessage()
            }
        }
    }
}

@Composable
private fun StoryAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(2.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                shadow = Shadow(color = Color.Black.copy(alpha = 0.6f), blurRadius = 8f),
            ),
            color = Color.White,
        )
    }
}

/** Pops the chosen reaction emoji in the centre of the story. */
@Composable
private fun EmojiBurst(
    emoji: String?,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (emoji == null) return
    val scale = remember(emoji) { Animatable(0.3f) }
    val alpha = remember(emoji) { Animatable(1f) }
    LaunchedEffect(emoji) {
        scale.animateTo(1.6f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium))
        alpha.animateTo(0f, tween(280, delayMillis = 120))
        onDone()
    }
    Text(
        text = emoji,
        style = MaterialTheme.typography.displaySmall,
        modifier = modifier
            .scale(scale.value * 2f)
            .alpha(alpha.value),
    )
}
