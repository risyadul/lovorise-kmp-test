package com.lovorise.discover.data.recommend

import com.lovorise.discover.data.model.FeedItem
import com.lovorise.discover.data.model.FeedPost
import com.lovorise.discover.data.model.ScoredProfile
import com.lovorise.discover.data.model.UserProfile
import com.lovorise.discover.data.model.Viewer
import kotlin.math.ln
import kotlin.math.max

/**
 * Simulated recommendation system. Profiles and posts are prioritised by
 * location, shared interests, profile quality and recent activity — the four
 * signals called out in the brief — and every score carries readable reasons
 * that the UI surfaces as "why you see this" chips.
 */
class RecommendationEngine {

    fun scoreProfile(viewer: Viewer, user: UserProfile): ScoredProfile {
        var score = 0.0
        val reasons = mutableListOf<String>()

        // Location -----------------------------------------------------------
        when {
            user.city.equals(viewer.city, ignoreCase = true) -> {
                score += 30.0
                reasons += "Near you"
            }
            user.country.equals(viewer.country, ignoreCase = true) -> score += 12.0
        }
        score += max(0.0, 18.0 - user.distanceKm / 60.0)

        // Interests ----------------------------------------------------------
        val shared = sharedInterests(viewer, user)
        if (shared.isNotEmpty()) {
            score += (shared.size * 12.0).coerceAtMost(36.0)
            reasons += if (shared.size == 1) {
                "Also into ${shared.first()}"
            } else {
                "${shared.size} shared interests"
            }
        }

        // Profile information --------------------------------------------------
        if (user.isVerified) {
            score += 8.0
            reasons += "Verified"
        }
        if (!user.photoUrl.isNullOrBlank()) score += 4.0
        if (user.bio.length > 40) score += 3.0

        // Recent activity ------------------------------------------------------
        when {
            user.lastActiveMinutes <= 20 -> {
                score += 15.0
                reasons += "Active now"
            }
            user.lastActiveMinutes <= 180 -> {
                score += 8.0
                reasons += "Active recently"
            }
            user.lastActiveMinutes <= 60 * 24 -> score += 3.0
        }

        return ScoredProfile(user = user, score = score, reasons = reasons)
    }

    /** Profiles worth recommending, best match first. Connected users are excluded. */
    fun rankProfiles(viewer: Viewer, users: List<UserProfile>): List<ScoredProfile> =
        users.asSequence()
            .filterNot { it.isConnected }
            .map { scoreProfile(viewer, it) }
            .sortedByDescending { it.score }
            .toList()

    /** Posts ordered by author affinity, freshness and engagement. */
    fun rankPosts(
        viewer: Viewer,
        posts: List<FeedPost>,
        usersById: Map<String, UserProfile>,
    ): List<FeedItem.Post> =
        posts.mapNotNull { post ->
            val author = usersById[post.authorId] ?: return@mapNotNull null
            val affinity = scoreProfile(viewer, author).score
            val freshness = max(0.0, 30.0 - ln(1.0 + post.postedMinutesAgo) * 4.5)
            val buzz = ln(1.0 + post.reactions) * 2.0
            val tagBonus = if (post.tags.any { tag -> viewer.interests.any { it.equals(tag, true) } }) 10.0 else 0.0
            Triple(post, author, affinity * 0.6 + freshness + buzz + tagBonus)
        }
            .sortedByDescending { it.third }
            .map { (post, author, _) -> FeedItem.Post(post, author, postReason(viewer, post, author)) }

    /** Short human-readable "why this is in your feed" label. */
    private fun postReason(viewer: Viewer, post: FeedPost, author: UserProfile): String? {
        val sharedTag = post.tags.firstOrNull { tag -> viewer.interests.any { it.equals(tag, true) } }
        return when {
            sharedTag != null -> "Because you like $sharedTag"
            author.city.equals(viewer.city, ignoreCase = true) -> "Popular near you"
            author.lastActiveMinutes <= 20 -> "Active now"
            sharedInterests(viewer, author).isNotEmpty() -> "Similar interests"
            else -> null
        }
    }

    /**
     * Weaves profile recommendations into the ranked post list so discovery
     * happens naturally while scrolling: a recommendation module appears
     * after every [every] posts, carrying [chunkSize] profiles.
     */
    fun interleave(
        posts: List<FeedItem.Post>,
        recommendations: List<ScoredProfile>,
        every: Int = 4,
        chunkSize: Int = 3,
        slotOffset: Int = 0,
    ): List<FeedItem> {
        if (recommendations.isEmpty()) return posts
        val chunks = recommendations.chunked(chunkSize)
        val result = mutableListOf<FeedItem>()
        var chunkIndex = 0
        posts.forEachIndexed { index, post ->
            result += post
            val position = index + 1
            if (position % every == 0 && chunkIndex < chunks.size) {
                result += FeedItem.ProfileRecs(
                    profiles = chunks[chunkIndex],
                    slot = slotOffset + chunkIndex,
                )
                chunkIndex++
            }
        }
        return result
    }

    fun sharedInterests(viewer: Viewer, user: UserProfile): List<String> =
        user.interests.filter { interest ->
            viewer.interests.any { it.equals(interest, ignoreCase = true) }
        }
}
