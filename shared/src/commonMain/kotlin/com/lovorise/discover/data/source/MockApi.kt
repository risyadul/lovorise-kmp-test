package com.lovorise.discover.data.source

import com.lovorise.discover.data.model.FeedPost
import com.lovorise.discover.data.model.Story
import com.lovorise.discover.data.model.UserProfile
import com.lovorise.discover.data.model.Viewer
import com.lovorise.discover.shared.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kotlin.random.Random

@Serializable
private data class UsersEnvelope(val viewer: Viewer, val users: List<UserProfile>)

@Serializable
private data class StoriesEnvelope(val stories: List<Story>)

@Serializable
private data class PostsEnvelope(val posts: List<FeedPost>)

/** Contract the repository depends on; lets tests substitute a fake backend. */
interface DiscoverApi {
    suspend fun fetchViewerAndUsers(): Pair<Viewer, List<UserProfile>>
    suspend fun fetchStories(): List<Story>
    suspend fun fetchFeedPage(page: Int, pageSize: Int): List<FeedPost>
}

/**
 * Mock API backed by JSON documents bundled as multiplatform compose
 * resources. Each call simulates network latency so loading, skeleton and
 * pagination states are honest on both Android and iOS.
 */
class MockApi(
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val latencyMillis: Long = 750L,
) : DiscoverApi {

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun <T> endpoint(file: String, parse: (String) -> T): T =
        withContext(Dispatchers.Default) {
            delay(latencyMillis)
            val raw = Res.readBytes("files/mock/$file").decodeToString()
            parse(raw)
        }

    override suspend fun fetchViewerAndUsers(): Pair<Viewer, List<UserProfile>> =
        endpoint("users.json") { raw ->
            val env = json.decodeFromString<UsersEnvelope>(raw)
            env.viewer to env.users
        }

    override suspend fun fetchStories(): List<Story> =
        endpoint("stories.json") { raw -> json.decodeFromString<StoriesEnvelope>(raw).stories }

    /**
     * Paged feed endpoint. Page 1 returns the full source catalogue so the
     * repository can rank it and keep the best [pageSize] posts; further
     * pages cycle the catalogue with fresh ids and jittered engagement so
     * the feed scrolls forever like a real recommendation backend.
     */
    override suspend fun fetchFeedPage(page: Int, pageSize: Int): List<FeedPost> =
        endpoint("posts.json") { raw ->
            val base = json.decodeFromString<PostsEnvelope>(raw).posts
            if (page <= 1) return@endpoint base

            val random = Random(page * 31L)
            val rotated = base.shuffled(random)
            rotated.take(pageSize).map { post ->
                post.copy(
                    id = "${post.id}-p$page",
                    postedMinutesAgo = post.postedMinutesAgo + page * 90 + random.nextInt(45),
                    reactions = (post.reactions * (0.55 + random.nextDouble() * 0.9)).toInt(),
                    shares = (post.shares * (0.5 + random.nextDouble())).toInt(),
                    saves = (post.saves * (0.5 + random.nextDouble())).toInt(),
                )
            }
        }
}
