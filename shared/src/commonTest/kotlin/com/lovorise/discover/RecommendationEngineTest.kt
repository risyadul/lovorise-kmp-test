package com.lovorise.discover

import com.lovorise.discover.data.model.FeedItem
import com.lovorise.discover.data.model.FeedPost
import com.lovorise.discover.data.model.UserProfile
import com.lovorise.discover.data.model.Viewer
import com.lovorise.discover.data.recommend.RecommendationEngine
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test

class RecommendationEngineTest {

    private val engine = RecommendationEngine()

    private val viewer = Viewer(
        name = "Rania",
        city = "Jakarta",
        country = "Indonesia",
        interests = listOf("Coffee", "Hiking", "Photography"),
    )

    private fun user(
        id: String,
        city: String = "Jakarta",
        distanceKm: Double = 5.0,
        interests: List<String> = emptyList(),
        verified: Boolean = false,
        lastActiveMinutes: Int = 9_999,
        connected: Boolean = false,
    ) = UserProfile(
        id = id,
        name = "User $id",
        age = 27,
        city = city,
        country = "Indonesia",
        distanceKm = distanceKm,
        bio = "A bio that is long enough to earn the completeness bonus points.",
        interests = interests,
        photoUrl = "https://example.com/$id.jpg",
        isVerified = verified,
        lastActiveMinutes = lastActiveMinutes,
        isConnected = connected,
    )

    @Test
    fun `nearby active user with shared interests outranks distant inactive stranger`() {
        val strongMatch = user(
            id = "strong",
            city = "Jakarta",
            distanceKm = 2.0,
            interests = listOf("Coffee", "Hiking"),
            lastActiveMinutes = 5,
        )
        val weakMatch = user(
            id = "weak",
            city = "Medan",
            distanceKm = 1_400.0,
            interests = listOf("Gaming"),
            lastActiveMinutes = 5_000,
        )

        val ranked = engine.rankProfiles(viewer, listOf(weakMatch, strongMatch))

        assertEquals("strong", ranked.first().user.id)
        assertTrue(ranked.first().score > ranked.last().score)
    }

    @Test
    fun `reasons explain location interests and activity signals`() {
        val scored = engine.scoreProfile(
            viewer,
            user(
                id = "x",
                city = "Jakarta",
                interests = listOf("Coffee", "Photography"),
                verified = true,
                lastActiveMinutes = 3,
            ),
        )

        assertTrue("Near you" in scored.reasons)
        assertTrue("2 shared interests" in scored.reasons)
        assertTrue("Verified" in scored.reasons)
        assertTrue("Active now" in scored.reasons)
    }

    @Test
    fun `connected users are excluded from recommendations`() {
        val ranked = engine.rankProfiles(
            viewer,
            listOf(user(id = "a", connected = true), user(id = "b")),
        )

        assertEquals(listOf("b"), ranked.map { it.user.id })
    }

    @Test
    fun `recent activity boosts an otherwise identical profile`() {
        val activeNow = engine.scoreProfile(viewer, user(id = "now", lastActiveMinutes = 5))
        val lastWeek = engine.scoreProfile(viewer, user(id = "week", lastActiveMinutes = 10_000))

        assertTrue(activeNow.score > lastWeek.score)
    }

    @Test
    fun `interleave inserts a recommendation module after every fourth post`() {
        val users = (1..8).map { user(id = "u$it") }
        val posts = users.mapIndexed { index, author ->
            FeedItem.Post(
                post = FeedPost(
                    id = "p$index",
                    authorId = author.id,
                    imageUrl = "https://example.com/p$index.jpg",
                    caption = "caption",
                    postedMinutesAgo = 10,
                    reactions = 10,
                    shares = 1,
                    saves = 1,
                ),
                author = author,
                reason = null,
            )
        }
        val recs = users.take(6).map { engine.scoreProfile(viewer, it) }

        val feed = engine.interleave(posts, recs, every = 4, chunkSize = 3)

        assertTrue(feed[4] is FeedItem.ProfileRecs)
        assertTrue(feed[9] is FeedItem.ProfileRecs)
        assertEquals(3, (feed[4] as FeedItem.ProfileRecs).profiles.size)
        assertEquals(posts.size, feed.filterIsInstance<FeedItem.Post>().size)
    }

    @Test
    fun `post ranking favours matching tags over raw engagement`() {
        val coffeeAuthor = user(id = "coffee", interests = listOf("Coffee"))
        val gamerAuthor = user(id = "gamer", city = "Medan", distanceKm = 1_400.0, interests = listOf("Gaming"), lastActiveMinutes = 8_000)
        val posts = listOf(
            FeedPost(
                id = "gaming-post",
                authorId = "gamer",
                imageUrl = "u",
                caption = "c",
                tags = listOf("Gaming"),
                postedMinutesAgo = 500,
                reactions = 5_000,
                shares = 10,
                saves = 10,
            ),
            FeedPost(
                id = "coffee-post",
                authorId = "coffee",
                imageUrl = "u",
                caption = "c",
                tags = listOf("Coffee"),
                postedMinutesAgo = 30,
                reactions = 100,
                shares = 5,
                saves = 5,
            ),
        )

        val ranked = engine.rankPosts(
            viewer,
            posts,
            mapOf("coffee" to coffeeAuthor, "gamer" to gamerAuthor),
        )

        assertEquals("coffee-post", ranked.first().post.id)
        assertEquals("Because you like Coffee", ranked.first().reason)
    }
}
