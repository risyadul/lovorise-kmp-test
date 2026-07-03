package com.lovorise.discover.data.repo

import com.lovorise.discover.data.model.FeedItem
import com.lovorise.discover.data.model.ScoredProfile
import com.lovorise.discover.data.model.Story
import com.lovorise.discover.data.model.StoryUi
import com.lovorise.discover.data.model.UserProfile
import com.lovorise.discover.data.model.Viewer
import com.lovorise.discover.data.recommend.RecommendationEngine
import com.lovorise.discover.data.search.UserSearchFilter
import com.lovorise.discover.data.source.DiscoverApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Per-session interaction state; every mutation returns a new copy. */
data class SessionState(
    val reactedPosts: Set<String> = emptySet(),
    val savedPosts: Set<String> = emptySet(),
    val savedStories: Set<String> = emptySet(),
    val sharedPosts: Set<String> = emptySet(),
    /** userId -> last message request text sent to them. */
    val messageRequests: Map<String, String> = emptyMap(),
    val requestedConnections: Set<String> = emptySet(),
    val connectedUsers: Set<String> = emptySet(),
    val incomingRequests: List<String> = emptyList(),
    val seenStories: Set<String> = emptySet(),
    val storyReactions: Map<String, String> = emptyMap(),
    val recentSearches: List<String> = emptyList(),
)

private val DEFAULT_RECENT_SEARCHES = listOf("Coffee date", "Salsabila", "Hiking buddy", "Live music")

/** How many posts a feed page carries once ranked. */
const val FEED_PAGE_SIZE = 8

/** A recommendation module is woven in after every [REC_EVERY] posts. */
private const val REC_EVERY = 4

/** Number of profiles inside one recommendation module. */
private const val REC_CHUNK_SIZE = 3

/**
 * Single source of truth for Discover. Loads the mock API once, keeps the
 * catalogue in memory and exposes all user interactions through [session].
 */
class DiscoverRepository(
    private val api: DiscoverApi,
    private val engine: RecommendationEngine = RecommendationEngine(),
) {

    private val loadMutex = Mutex()
    private var initialized = false
    private var cachedViewer: Viewer? = null
    private var cachedUsers: List<UserProfile> = emptyList()
    private var cachedUsersById: Map<String, UserProfile> = emptyMap()
    private var cachedStories: List<Story> = emptyList()

    private val _session = MutableStateFlow(SessionState(recentSearches = DEFAULT_RECENT_SEARCHES))
    val session: StateFlow<SessionState> = _session.asStateFlow()

    val viewer: Viewer
        get() = cachedViewer ?: Viewer(
            name = "You",
            city = "Jakarta",
            country = "Indonesia",
            interests = emptyList(),
        )

    /**
     * Loads (or returns cached) viewer, users and stories from the mock API.
     * All fetches complete before any state is committed, so a failure
     * mid-way leaves the repository unloaded and the next call retries.
     */
    suspend fun warmUp() {
        loadMutex.withLock {
            if (initialized) return
            val (viewer, users) = api.fetchViewerAndUsers()
            val stories = api.fetchStories()
            cachedViewer = viewer
            cachedUsers = users
            cachedUsersById = users.associateBy { it.id }
            cachedStories = stories
            _session.update { state ->
                state.copy(
                    connectedUsers = users.filter { it.isConnected }.map { it.id }.toSet(),
                    incomingRequests = users.filter { it.hasIncomingRequest }.map { it.id },
                )
            }
            initialized = true
        }
    }

    fun userById(id: String): UserProfile? = cachedUsersById[id]

    fun sharedInterestsWith(user: UserProfile): List<String> = engine.sharedInterests(viewer, user)

    // Feed ---------------------------------------------------------------

    /**
     * One ranked feed page. For the For You feed the page is interleaved
     * with recommendation modules; with [connectionsOnly] the page contains
     * only posts from connected users and no recommendation modules —
     * the Connections tab queries the data layer, it does not filter the UI.
     */
    suspend fun feedPage(
        page: Int,
        pageSize: Int = FEED_PAGE_SIZE,
        connectionsOnly: Boolean = false,
    ): List<FeedItem> {
        warmUp()
        val posts = api.fetchFeedPage(page, pageSize)
        val ranked = engine.rankPosts(viewer, posts, cachedUsersById)

        if (connectionsOnly) {
            val connected = _session.value.connectedUsers
            return ranked.filter { it.post.authorId in connected }.take(pageSize)
        }

        val pagePosts = ranked.take(pageSize)
        val recommendations = recommendedProfiles()
        // Every page consumes chunksPerPage modules; slots therefore stay
        // globally unique and the rotation resumes where the last page ended.
        val chunksPerPage = pageSize / REC_EVERY
        val startChunk = (page - 1) * chunksPerPage
        val poolSize = recommendations.size.coerceAtLeast(1)
        val offset = (startChunk * REC_CHUNK_SIZE) % poolSize
        val rotating = recommendations.drop(offset) + recommendations.take(offset)
        return engine.interleave(
            posts = pagePosts,
            recommendations = rotating,
            every = REC_EVERY,
            chunkSize = REC_CHUNK_SIZE,
            slotOffset = startChunk,
        )
    }

    /** Ranked profile recommendations, excluding users already connected. */
    fun recommendedProfiles(): List<ScoredProfile> {
        val state = _session.value
        return engine.rankProfiles(viewer, cachedUsers)
            .filterNot { it.user.id in state.connectedUsers }
    }

    // Stories ------------------------------------------------------------

    /** Stories with their author, unseen first then most recent. */
    fun stories(connectionsOnly: Boolean = false): List<StoryUi> {
        val state = _session.value
        return cachedStories.mapNotNull { story ->
            val user = userById(story.userId) ?: return@mapNotNull null
            if (connectionsOnly && story.userId !in state.connectedUsers) return@mapNotNull null
            StoryUi(story = story, user = user, seen = story.id in state.seenStories)
        }.sortedWith(compareBy({ it.seen }, { it.story.postedMinutesAgo }))
    }

    fun markStorySeen(storyId: String) {
        _session.update { it.copy(seenStories = it.seenStories + storyId) }
    }

    fun reactToStory(storyId: String, emoji: String) {
        _session.update { it.copy(storyReactions = it.storyReactions + (storyId to emoji)) }
    }

    fun toggleStorySave(storyId: String) {
        _session.update {
            val next = if (storyId in it.savedStories) it.savedStories - storyId else it.savedStories + storyId
            it.copy(savedStories = next)
        }
    }

    // Post interactions ----------------------------------------------------

    fun toggleReaction(postId: String) {
        _session.update {
            val next = if (postId in it.reactedPosts) it.reactedPosts - postId else it.reactedPosts + postId
            it.copy(reactedPosts = next)
        }
    }

    fun toggleSave(postId: String) {
        _session.update {
            val next = if (postId in it.savedPosts) it.savedPosts - postId else it.savedPosts + postId
            it.copy(savedPosts = next)
        }
    }

    fun markShared(postId: String) {
        _session.update { it.copy(sharedPosts = it.sharedPosts + postId) }
    }

    // Connections ----------------------------------------------------------

    fun requestConnection(userId: String) {
        _session.update {
            val next = if (userId in it.requestedConnections) {
                it.requestedConnections - userId
            } else {
                it.requestedConnections + userId
            }
            it.copy(requestedConnections = next)
        }
    }

    fun acceptRequest(userId: String) {
        _session.update {
            it.copy(
                incomingRequests = it.incomingRequests - userId,
                connectedUsers = it.connectedUsers + userId,
            )
        }
    }

    fun declineRequest(userId: String) {
        _session.update { it.copy(incomingRequests = it.incomingRequests - userId) }
    }

    fun sendMessageRequest(userId: String, message: String) {
        _session.update {
            it.copy(messageRequests = it.messageRequests + (userId to message.trim()))
        }
    }

    // Search ----------------------------------------------------------------

    /** Local, case-insensitive filtering across name, city, bio and interests. */
    fun searchUsers(query: String): List<UserProfile> =
        UserSearchFilter.filter(cachedUsers, query)

    fun commitRecentSearch(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        _session.update { state ->
            val next = (listOf(q) + state.recentSearches.filterNot { it.equals(q, true) }).take(8)
            state.copy(recentSearches = next)
        }
    }

    fun removeRecentSearch(query: String) {
        _session.update { state ->
            state.copy(recentSearches = state.recentSearches.filterNot { it.equals(query, true) })
        }
    }

    fun clearRecentSearches() {
        _session.update { it.copy(recentSearches = emptyList()) }
    }
}
