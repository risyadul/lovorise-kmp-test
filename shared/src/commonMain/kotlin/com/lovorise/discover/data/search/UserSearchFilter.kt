package com.lovorise.discover.data.search

import com.lovorise.discover.data.model.UserProfile

/** Pure local search over the in-memory user catalogue. */
object UserSearchFilter {

    fun filter(users: List<UserProfile>, query: String): List<UserProfile> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        return users.filter { user -> matches(user, q) }
    }

    fun matches(user: UserProfile, query: String): Boolean =
        user.name.contains(query, ignoreCase = true) ||
            user.city.contains(query, ignoreCase = true) ||
            user.bio.contains(query, ignoreCase = true) ||
            user.interests.any { it.contains(query, ignoreCase = true) }
}
