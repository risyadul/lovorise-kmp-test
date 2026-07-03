package com.lovorise.discover.feature.search

import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lovorise.discover.core.designsystem.LovoriseColors
import com.lovorise.discover.core.designsystem.components.Avatar
import com.lovorise.discover.core.designsystem.components.LovorisePillButton
import com.lovorise.discover.core.util.formatDistance
import com.lovorise.discover.data.model.UserProfile

/**
 * Search flow: auto-focused input, recent searches, suggested people from the
 * recommendation engine, and instant local filtering.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenStory: (String) -> Unit,
    viewModel: SearchViewModel = viewModel(factory = SearchViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    BackHandler(onBack = onBack)

    Scaffold(
        containerColor = Color.White,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .statusBarsPadding()
                    .padding(start = 4.dp, end = 16.dp, top = 6.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back to home",
                        tint = LovoriseColors.Ink,
                    )
                }
                SearchField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    onSubmit = viewModel::submitSearch,
                    onClear = { viewModel.onQueryChange("") },
                    focusRequester = focusRequester,
                    modifier = Modifier.weight(1f),
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (state.query.isBlank()) {
                if (state.recentSearches.isNotEmpty()) {
                    item(key = "recent-header") {
                        SectionHeader(
                            title = "Recent searches",
                            action = "Clear all",
                            onAction = viewModel::clearRecents,
                        )
                    }
                    item(key = "recent-chips") {
                        FlowRow(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            state.recentSearches.forEach { term ->
                                RecentChip(
                                    term = term,
                                    onClick = { viewModel.selectRecent(term) },
                                    onRemove = { viewModel.removeRecent(term) },
                                )
                            }
                        }
                    }
                }
                item(key = "suggested-header") {
                    SectionHeader(title = "Suggested for you")
                }
                suggestedItems(state, viewModel, onOpenStory)
            } else {
                item(key = "results-header") {
                    SectionHeader(
                        title = if (state.results.isEmpty()) "No matches for \"${state.query}\""
                        else "People (${state.results.size})",
                    )
                }
                if (state.results.isEmpty()) {
                    item(key = "empty") { EmptyResults(state.query) }
                } else {
                    resultItems(state, viewModel, onOpenStory)
                }
            }
        }
    }
}

// Extracted so both sections reuse the same row rendering.
private fun androidx.compose.foundation.lazy.LazyListScope.suggestedItems(
    state: SearchUiState,
    viewModel: SearchViewModel,
    onOpenStory: (String) -> Unit,
) {
    state.suggested.forEach { scored ->
        item(key = "suggested-${scored.user.id}") {
            val storyId = viewModel.storyIdFor(scored.user.id)
            UserRow(
                user = scored.user,
                subtitle = scored.reasons.take(2).joinToString(" • ")
                    .ifBlank { scored.user.bio },
                requested = scored.user.id in state.requestedConnections,
                connected = scored.user.id in state.connectedUsers,
                onConnect = { viewModel.requestConnection(scored.user) },
                onClick = storyId?.let { id -> { onOpenStory(id) } },
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.resultItems(
    state: SearchUiState,
    viewModel: SearchViewModel,
    onOpenStory: (String) -> Unit,
) {
    state.results.forEach { user ->
        item(key = "result-${user.id}") {
            val storyId = viewModel.storyIdFor(user.id)
            UserRow(
                user = user,
                subtitle = "${user.city} • ${user.interests.take(3).joinToString(", ")}",
                requested = user.id in state.requestedConnections,
                connected = user.id in state.connectedUsers,
                onConnect = { viewModel.requestConnection(user) },
                onClick = storyId?.let { id -> { onOpenStory(id) } },
            )
        }
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onClear: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(LovoriseColors.SurfaceDim)
            .padding(horizontal = 14.dp)
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = LovoriseColors.Muted,
            modifier = Modifier.size(20.dp),
        )
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = "Search people, interests, cities…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LovoriseColors.Muted,
                    maxLines = 1,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = LovoriseColors.Ink),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
        }
        if (value.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClear),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Clear search",
                    tint = LovoriseColors.Muted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = LovoriseColors.Ink,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (action != null && onAction != null) {
            Text(
                text = action,
                style = MaterialTheme.typography.labelLarge,
                color = LovoriseColors.Pink,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun RecentChip(
    term: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(LovoriseColors.SurfaceDim)
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.History,
            contentDescription = null,
            tint = LovoriseColors.Muted,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = term,
            style = MaterialTheme.typography.labelMedium,
            color = LovoriseColors.Slate,
        )
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Remove $term from recent searches",
                tint = LovoriseColors.Muted,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun UserRow(
    user: UserProfile,
    subtitle: String,
    requested: Boolean,
    connected: Boolean,
    onConnect: () -> Unit,
    /** Null when the user has no story — the row then isn't tappable. */
    onClick: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Avatar(name = user.name, imageUrl = user.photoUrl, size = 48.dp)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${user.name}, ${user.age}",
                    style = MaterialTheme.typography.titleSmall,
                    color = LovoriseColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (user.isVerified) {
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = "Verified",
                        tint = LovoriseColors.Pink,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(13.dp),
                    )
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = LovoriseColors.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatDistance(user.distanceKm),
                style = MaterialTheme.typography.labelSmall,
                color = LovoriseColors.Muted,
            )
        }
        when {
            connected -> Text(
                text = "Connected",
                style = MaterialTheme.typography.labelMedium,
                color = LovoriseColors.Success,
            )
            else -> LovorisePillButton(
                text = if (requested) "Requested" else "Connect",
                icon = if (requested) Icons.Outlined.Check else null,
                filled = !requested,
                compact = true,
                onClick = onConnect,
            )
        }
    }
}

@Composable
private fun EmptyResults(query: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "🔍", style = MaterialTheme.typography.displaySmall)
        Text(
            text = "Nothing found for \"$query\"",
            style = MaterialTheme.typography.titleMedium,
            color = LovoriseColors.Ink,
        )
        Text(
            text = "Try a name, a city like \"Bandung\", or an interest like \"Coffee\".",
            style = MaterialTheme.typography.bodyMedium,
            color = LovoriseColors.Slate,
        )
    }
}
