package com.lovorise.discover.feature.home

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lovorise.discover.AppGraph
import com.lovorise.discover.core.ui.BaseDiscoverViewModel
import com.lovorise.discover.data.model.FeedItem
import com.lovorise.discover.data.model.StoryUi
import com.lovorise.discover.data.model.UserProfile
import com.lovorise.discover.data.repo.DiscoverRepository
import com.lovorise.discover.data.repo.SessionState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class HomeTab { FOR_YOU, CONNECTIONS }

data class HomeUiState(
    val isLoading: Boolean = true,
    val isConnectionsLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val tab: HomeTab = HomeTab.FOR_YOU,
    val stories: List<StoryUi> = emptyList(),
    /** Story lookup for post-author rings; avoids per-card catalogue scans. */
    val storyByAuthor: Map<String, StoryUi> = emptyMap(),
    val feed: List<FeedItem> = emptyList(),
    val connectionRequests: List<UserProfile> = emptyList(),
    val session: SessionState = SessionState(),
    val notificationCount: Int = 0,
    val viewerName: String = "You",
    val viewerPhotoUrl: String? = null,
)

private data class LoadFlags(
    val initial: Boolean = true,
    val connectionsInitial: Boolean = false,
    val more: Boolean = false,
    val refreshing: Boolean = false,
)

/** Give a thin, empty page a few chances to fill up before giving up. */
private const val MAX_LOAD_MORE_ATTEMPTS = 3

class HomeViewModel(repo: DiscoverRepository) : BaseDiscoverViewModel(repo) {

    private val tab = MutableStateFlow(HomeTab.FOR_YOU)
    private val forYouFeed = MutableStateFlow<List<FeedItem>>(emptyList())
    private val connectionsFeed = MutableStateFlow<List<FeedItem>>(emptyList())
    private val flags = MutableStateFlow(LoadFlags())

    private var forYouNextPage = 1
    private var connectionsNextPage = 1
    private var connectionsStarted = false

    val uiState: StateFlow<HomeUiState> =
        combine(tab, forYouFeed, connectionsFeed, flags, repo.session) { tab, forYou, connections, flags, session ->
            val allStories = repo.stories()
            HomeUiState(
                isLoading = flags.initial,
                isConnectionsLoading = flags.connectionsInitial,
                isLoadingMore = flags.more,
                isRefreshing = flags.refreshing,
                tab = tab,
                stories = if (tab == HomeTab.CONNECTIONS) {
                    allStories.filter { it.user.id in session.connectedUsers }
                } else {
                    allStories
                },
                storyByAuthor = allStories.associateBy { it.user.id },
                feed = if (tab == HomeTab.FOR_YOU) forYou else connections,
                connectionRequests = session.incomingRequests.mapNotNull(repo::userById),
                session = session,
                notificationCount = session.incomingRequests.size,
                viewerName = repo.viewer.name,
                viewerPhotoUrl = repo.viewer.photoUrl,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        loadInitial()
    }

    private fun loadInitial() {
        viewModelScope.launch {
            flags.update { it.copy(initial = true) }
            try {
                forYouFeed.value = repo.feedPage(page = 1)
                forYouNextPage = 2
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                sendEvent("Couldn't load your feed — pull down to retry")
            } finally {
                flags.update { it.copy(initial = false) }
            }
        }
    }

    fun refresh() {
        val current = flags.value
        if (current.refreshing || current.more || current.initial) return
        viewModelScope.launch {
            flags.update { it.copy(refreshing = true) }
            try {
                if (tab.value == HomeTab.FOR_YOU) {
                    forYouFeed.value = repo.feedPage(page = 1)
                    forYouNextPage = 2
                } else {
                    connectionsFeed.value = repo.feedPage(page = 1, connectionsOnly = true)
                    connectionsNextPage = 2
                }
                sendEvent("Feed refreshed with new recommendations")
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                sendEvent("Couldn't refresh — check back in a moment")
            } finally {
                flags.update { it.copy(refreshing = false) }
            }
        }
    }

    fun loadMore() {
        val current = flags.value
        if (current.initial || current.connectionsInitial || current.more || current.refreshing) return
        viewModelScope.launch {
            flags.update { it.copy(more = true) }
            try {
                // Connections pages can come back empty after filtering, so
                // retry a few pages before yielding back to the scroller.
                var appended = 0
                var attempts = 0
                while (appended == 0 && attempts < MAX_LOAD_MORE_ATTEMPTS) {
                    appended = if (tab.value == HomeTab.FOR_YOU) {
                        val items = repo.feedPage(page = forYouNextPage)
                        forYouNextPage++
                        forYouFeed.update { it + items }
                        items.size
                    } else {
                        val items = repo.feedPage(page = connectionsNextPage, connectionsOnly = true)
                        connectionsNextPage++
                        connectionsFeed.update { it + items }
                        items.size
                    }
                    attempts++
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                sendEvent("Couldn't load more right now")
            } finally {
                flags.update { it.copy(more = false) }
            }
        }
    }

    fun selectTab(newTab: HomeTab) {
        tab.value = newTab
        if (newTab == HomeTab.CONNECTIONS) ensureConnectionsLoaded()
    }

    private fun ensureConnectionsLoaded(silent: Boolean = false) {
        if (connectionsStarted && !silent) return
        connectionsStarted = true
        viewModelScope.launch {
            if (!silent) flags.update { it.copy(connectionsInitial = true) }
            try {
                connectionsFeed.value = repo.feedPage(page = 1, connectionsOnly = true)
                connectionsNextPage = 2
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                if (!silent) sendEvent("Couldn't load connections — pull down to retry")
            } finally {
                if (!silent) flags.update { it.copy(connectionsInitial = false) }
            }
        }
    }

    fun onNotificationsClick() = sendEvent("Notifications are UI-only in this prototype")

    fun onAddStory() = sendEvent("Story creation is coming soon ✨")

    fun onPostOptions() = sendEvent("Post options are UI-only in this prototype")

    fun sharedInterests(user: UserProfile): List<String> = repo.sharedInterestsWith(user)

    fun toggleReaction(postId: String) = repo.toggleReaction(postId)

    fun reactIfNeeded(postId: String) {
        if (postId !in repo.session.value.reactedPosts) repo.toggleReaction(postId)
    }

    fun toggleSave(postId: String) {
        repo.toggleSave(postId)
        val saved = postId in repo.session.value.savedPosts
        sendEvent(if (saved) "Saved to your collection" else "Removed from saved")
    }

    fun markShared(postId: String) = repo.markShared(postId)

    fun sendMessageRequest(user: UserProfile, message: String) {
        repo.sendMessageRequest(user.id, message)
        sendEvent("Message request sent to ${user.name.substringBefore(' ')}")
    }

    fun acceptRequest(user: UserProfile) {
        repo.acceptRequest(user.id)
        // The new connection's posts belong in the Connections feed right away.
        if (connectionsStarted) ensureConnectionsLoaded(silent = true)
        sendEvent("You are now connected with ${user.name.substringBefore(' ')}")
    }

    fun declineRequest(user: UserProfile) = repo.declineRequest(user.id)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HomeViewModel(AppGraph.repository)
            }
        }
    }
}
