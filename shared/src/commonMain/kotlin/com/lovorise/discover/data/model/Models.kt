package com.lovorise.discover.data.model

import kotlinx.serialization.Serializable

/** The signed-in user, used to drive the recommendation simulation. */
@Serializable
data class Viewer(
    val name: String,
    val city: String,
    val country: String,
    val interests: List<String>,
    val photoUrl: String? = null,
)

@Serializable
data class UserProfile(
    val id: String,
    val name: String,
    val age: Int,
    val city: String,
    val country: String,
    val distanceKm: Double,
    val bio: String,
    val interests: List<String>,
    val photoUrl: String? = null,
    val isVerified: Boolean = false,
    /** Minutes since the user was last active; feeds the recency signal. */
    val lastActiveMinutes: Int = 9_999,
    /** Already-connected users power the Connections tab. */
    val isConnected: Boolean = false,
    /** Users with a pending request appear in the Connections request strip. */
    val hasIncomingRequest: Boolean = false,
)

@Serializable
data class Story(
    val id: String,
    val userId: String,
    val images: List<String>,
    val postedMinutesAgo: Int,
)

@Serializable
data class FeedPost(
    val id: String,
    val authorId: String,
    val imageUrl: String,
    val caption: String,
    val tags: List<String> = emptyList(),
    val location: String? = null,
    val postedMinutesAgo: Int,
    val reactions: Int,
    val shares: Int,
    val saves: Int,
)

/** A ranked profile plus the human-readable reasons behind its score. */
data class ScoredProfile(
    val user: UserProfile,
    val score: Double,
    val reasons: List<String>,
)

/** Items the Discover feed can render, in ranked order. */
sealed interface FeedItem {
    val key: String

    data class Post(
        val post: FeedPost,
        val author: UserProfile,
        val reason: String?,
    ) : FeedItem {
        override val key: String get() = "post-${post.id}"
    }

    data class ProfileRecs(
        val profiles: List<ScoredProfile>,
        val slot: Int,
    ) : FeedItem {
        override val key: String get() = "recs-$slot"
    }
}

data class StoryUi(
    val story: Story,
    val user: UserProfile,
    val seen: Boolean,
)
