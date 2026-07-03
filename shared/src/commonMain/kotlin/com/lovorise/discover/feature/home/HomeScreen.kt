package com.lovorise.discover.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.filter
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lovorise.discover.core.designsystem.LovoriseColors
import com.lovorise.discover.core.platform.rememberShareText
import com.lovorise.discover.core.designsystem.components.LovorisePillButton
import com.lovorise.discover.data.model.FeedItem
import com.lovorise.discover.data.model.UserProfile
import com.lovorise.discover.feature.home.components.FeedCardSkeleton
import com.lovorise.discover.feature.home.components.FeedPostCard
import com.lovorise.discover.feature.home.components.HomeHeader
import com.lovorise.discover.feature.home.components.MessageRequestSheet
import com.lovorise.discover.feature.home.components.ProfileRecsSection
import com.lovorise.discover.feature.home.components.RequestsStrip
import com.lovorise.discover.feature.home.components.StoriesRow
import com.lovorise.discover.feature.home.components.StoriesRowSkeleton

/**
 * Redesigned Discover home: For You / Connections tabs, a stories feed,
 * ranked content cards with the action sidebar, recommended profiles woven
 * into the feed, pull-to-refresh and infinite scrolling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenSearch: () -> Unit,
    onOpenStory: (storyId: String, connectionsOnly: Boolean) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val shareText = rememberShareText()
    var messageTarget by remember { mutableStateOf<UserProfile?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { snackbarHostState.showSnackbar(it) }
    }

    fun sharePost(item: FeedItem.Post) {
        viewModel.markShared(item.post.id)
        shareText("Check out ${item.author.name}'s moment on Lovorise 💗\n\n\"${item.post.caption}\"")
    }

    fun shareProfile(user: UserProfile) {
        shareText("Meet ${user.name} on Lovorise 💗 — ${user.bio}")
    }

    Scaffold(
        containerColor = LovoriseColors.Surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .statusBarsPadding(),
            ) {
                HomeHeader(
                    tab = state.tab,
                    notificationCount = state.notificationCount,
                    onTabSelected = viewModel::selectTab,
                    onSearchClick = onOpenSearch,
                    onNotificationsClick = viewModel::onNotificationsClick,
                )
            }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (state.isLoading) {
                LoadingSkeleton()
            } else {
                FeedList(
                    state = state,
                    onOpenStory = onOpenStory,
                    onOpenSearch = onOpenSearch,
                    viewModel = viewModel,
                    onSharePost = ::sharePost,
                    onShareProfile = ::shareProfile,
                    onMessageRequest = { messageTarget = it },
                )
            }
        }
    }

    messageTarget?.let { target ->
        MessageRequestSheet(
            user = target,
            onDismiss = { messageTarget = null },
            onSend = { text ->
                viewModel.sendMessageRequest(target, text)
                messageTarget = null
            },
        )
    }
}

@Composable
private fun FeedList(
    state: HomeUiState,
    onOpenStory: (storyId: String, connectionsOnly: Boolean) -> Unit,
    onOpenSearch: () -> Unit,
    viewModel: HomeViewModel,
    onSharePost: (FeedItem.Post) -> Unit,
    onShareProfile: (UserProfile) -> Unit,
    onMessageRequest: (UserProfile) -> Unit,
) {
    // Each tab keeps its own scroll position.
    val forYouListState = rememberLazyListState()
    val connectionsListState = rememberLazyListState()
    val listState = if (state.tab == HomeTab.FOR_YOU) forYouListState else connectionsListState
    InfiniteScrollEffect(listState, onLoadMore = viewModel::loadMore)
    val connectionsOnly = state.tab == HomeTab.CONNECTIONS

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item(key = "stories") {
            Column(Modifier.fillMaxWidth().background(Color.White)) {
                StoriesRow(
                    stories = state.stories,
                    viewerName = state.viewerName,
                    viewerPhotoUrl = state.viewerPhotoUrl,
                    onStoryClick = { onOpenStory(it.story.id, connectionsOnly) },
                    onAddStoryClick = viewModel::onAddStory,
                )
                HorizontalDivider(color = LovoriseColors.Border, thickness = 1.dp)
            }
        }

        if (state.tab == HomeTab.CONNECTIONS && state.connectionRequests.isNotEmpty()) {
            item(key = "requests") {
                RequestsStrip(
                    requests = state.connectionRequests,
                    onAccept = viewModel::acceptRequest,
                    onDecline = viewModel::declineRequest,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        if (state.tab == HomeTab.CONNECTIONS && state.feed.isEmpty()) {
            if (state.isConnectionsLoading) {
                item(key = "connections-loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = LovoriseColors.Pink,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            } else {
                item(key = "empty-connections") {
                    EmptyConnectionsState(onDiscover = { viewModel.selectTab(HomeTab.FOR_YOU) })
                }
            }
        }

        items(
            items = state.feed,
            key = { it.key },
            contentType = { if (it is FeedItem.Post) "post" else "recs" },
        ) { item ->
            when (item) {
                is FeedItem.Post -> {
                    val authorStory = state.storyByAuthor[item.author.id]
                    FeedPostCard(
                    item = item,
                    reacted = item.post.id in state.session.reactedPosts,
                    saved = item.post.id in state.session.savedPosts,
                    sharedLocally = item.post.id in state.session.sharedPosts,
                    hasStory = authorStory != null,
                    storySeen = authorStory?.seen ?: false,
                    onAuthorClick = {
                        authorStory?.let { onOpenStory(it.story.id, connectionsOnly) }
                    },
                    onReact = { viewModel.toggleReaction(item.post.id) },
                    onDoubleTapReact = { viewModel.reactIfNeeded(item.post.id) },
                    onMessage = { onMessageRequest(item.author) },
                    onShare = { onSharePost(item) },
                    onSave = { viewModel.toggleSave(item.post.id) },
                    onMore = viewModel::onPostOptions,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }

                is FeedItem.ProfileRecs -> ProfileRecsSection(
                    item = item,
                    requestedConnections = state.session.requestedConnections,
                    sharedInterestsOf = viewModel::sharedInterests,
                    onConnect = viewModel::requestConnection,
                    onMessage = onMessageRequest,
                    onShareProfile = onShareProfile,
                    onSeeAll = onOpenSearch,
                )
            }
        }

        item(key = "footer") {
            if (state.isLoadingMore) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = LovoriseColors.Pink,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(28.dp),
                    )
                }
            } else {
                Box(Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Triggers [onLoadMore] whenever the list nears its end. The snapshot pairs
 * the condition with the item count so appends re-arm the trigger even when
 * the condition itself never flips back to false (short filtered lists).
 */
@Composable
private fun InfiniteScrollEffect(listState: LazyListState, onLoadMore: () -> Unit) {
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            val nearEnd = info.totalItemsCount > 0 && lastVisible >= info.totalItemsCount - 3
            nearEnd to info.totalItemsCount
        }
            .filter { it.first }
            .collect { onLoadMore() }
    }
}

@Composable
private fun LoadingSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState(), enabled = false),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.fillMaxWidth().background(Color.White)) {
            StoriesRowSkeleton()
            HorizontalDivider(color = LovoriseColors.Border, thickness = 1.dp)
        }
        FeedCardSkeleton(Modifier.padding(horizontal = 16.dp))
        FeedCardSkeleton(Modifier.padding(horizontal = 16.dp))
    }
}

@Composable
private fun EmptyConnectionsState(onDiscover: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(text = "🫶", style = MaterialTheme.typography.displaySmall)
        Text(
            text = "No posts from connections yet",
            style = MaterialTheme.typography.titleMedium,
            color = LovoriseColors.Ink,
        )
        Text(
            text = "Accept requests or connect with people in For You — their moments will show up here.",
            style = MaterialTheme.typography.bodyMedium,
            color = LovoriseColors.Slate,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        LovorisePillButton(text = "Discover people", onClick = onDiscover)
    }
}
