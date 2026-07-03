package com.lovorise.discover

import com.lovorise.discover.data.model.FeedItem
import com.lovorise.discover.data.model.FeedPost
import com.lovorise.discover.data.model.Story
import com.lovorise.discover.data.model.UserProfile
import com.lovorise.discover.data.model.Viewer
import com.lovorise.discover.data.repo.DiscoverRepository
import com.lovorise.discover.data.source.DiscoverApi
import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.Test

private class FakeApi(
    var failStoriesOnce: Boolean = false,
) : DiscoverApi {

    val viewer = Viewer(
        name = "Rania",
        city = "Jakarta",
        country = "Indonesia",
        interests = listOf("Coffee", "Hiking"),
    )

    val users: List<UserProfile> = (1..14).map { n ->
        UserProfile(
            id = "u$n",
            name = "User $n",
            age = 25,
            city = if (n % 2 == 0) "Jakarta" else "Bandung",
            country = "Indonesia",
            distanceKm = n.toDouble(),
            bio = "A bio long enough for the completeness bonus to apply.",
            interests = if (n % 3 == 0) listOf("Coffee") else listOf("Gaming"),
            lastActiveMinutes = n * 10,
            isConnected = n <= 3,
        )
    }

    private val basePosts: List<FeedPost> = (1..14).map { n ->
        FeedPost(
            id = "p$n",
            authorId = "u$n",
            imageUrl = "https://example.com/p$n.jpg",
            caption = "caption $n",
            postedMinutesAgo = n * 15,
            reactions = 100 + n,
            shares = n,
            saves = n,
        )
    }

    override suspend fun fetchViewerAndUsers(): Pair<Viewer, List<UserProfile>> = viewer to users

    override suspend fun fetchStories(): List<Story> {
        if (failStoriesOnce) {
            failStoriesOnce = false
            throw IllegalStateException("boom")
        }
        return listOf(
            Story(id = "s1", userId = "u1", images = listOf("a", "b"), postedMinutesAgo = 10),
            Story(id = "s2", userId = "u5", images = listOf("a"), postedMinutesAgo = 20),
        )
    }

    override suspend fun fetchFeedPage(page: Int, pageSize: Int): List<FeedPost> =
        if (page <= 1) {
            basePosts
        } else {
            basePosts.take(pageSize).map { it.copy(id = "${it.id}-p$page") }
        }
}

class DiscoverRepositoryTest {

    @Test
    fun `feed item keys stay unique across pages`() = runTest {
        val repo = DiscoverRepository(api = FakeApi())

        val feed = repo.feedPage(1) + repo.feedPage(2) + repo.feedPage(3)
        val keys = feed.map { it.key }

        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun `recommendation modules rotate across the page boundary`() = runTest {
        val repo = DiscoverRepository(api = FakeApi())

        val page1Recs = repo.feedPage(1).filterIsInstance<FeedItem.ProfileRecs>()
        val page2Recs = repo.feedPage(2).filterIsInstance<FeedItem.ProfileRecs>()
        assertTrue(page1Recs.isNotEmpty() && page2Recs.isNotEmpty())

        val lastOfPage1 = page1Recs.last().profiles.map { it.user.id }
        val firstOfPage2 = page2Recs.first().profiles.map { it.user.id }

        assertNotEquals(lastOfPage1, firstOfPage2)
    }

    @Test
    fun `every page carries the same number of posts`() = runTest {
        val repo = DiscoverRepository(api = FakeApi())

        val page1Posts = repo.feedPage(1).filterIsInstance<FeedItem.Post>()
        val page2Posts = repo.feedPage(2).filterIsInstance<FeedItem.Post>()

        assertEquals(page2Posts.size, page1Posts.size)
    }

    @Test
    fun `connections feed contains only connected authors and no rec modules`() = runTest {
        val repo = DiscoverRepository(api = FakeApi())

        val feed = repo.feedPage(1, connectionsOnly = true)

        assertTrue(feed.isNotEmpty())
        assertTrue(feed.all { it is FeedItem.Post })
        val connected = setOf("u1", "u2", "u3")
        assertTrue(feed.filterIsInstance<FeedItem.Post>().all { it.post.authorId in connected })
    }

    @Test
    fun `warmUp commits nothing on partial failure and succeeds on retry`() = runTest {
        val api = FakeApi(failStoriesOnce = true)
        val repo = DiscoverRepository(api = api)

        val firstAttempt = runCatching { repo.warmUp() }
        assertTrue(firstAttempt.isFailure)
        assertTrue(repo.stories().isEmpty())
        assertTrue(repo.session.value.connectedUsers.isEmpty())

        repo.warmUp()
        assertEquals(2, repo.stories().size)
        assertEquals(setOf("u1", "u2", "u3"), repo.session.value.connectedUsers)
    }

    @Test
    fun `message request stores the composed text`() = runTest {
        val repo = DiscoverRepository(api = FakeApi())
        repo.warmUp()

        repo.sendMessageRequest("u4", "Coffee sometime? ")

        assertEquals("Coffee sometime?", repo.session.value.messageRequests["u4"])
    }

    @Test
    fun `saving a story does not mark any post as saved`() = runTest {
        val repo = DiscoverRepository(api = FakeApi())
        repo.warmUp()

        repo.toggleStorySave("s1")

        assertTrue("s1" in repo.session.value.savedStories)
        assertTrue(repo.session.value.savedPosts.isEmpty())
    }
}
