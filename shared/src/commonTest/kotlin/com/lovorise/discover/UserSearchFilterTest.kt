package com.lovorise.discover

import com.lovorise.discover.data.model.UserProfile
import com.lovorise.discover.data.search.UserSearchFilter
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test

class UserSearchFilterTest {

    private val users = listOf(
        profile(id = "1", name = "Salsabila Rahma", city = "Jakarta", bio = "Coffee snob", interests = listOf("Coffee", "Yoga")),
        profile(id = "2", name = "Keisha Amelia", city = "Bandung", bio = "Front row at gigs", interests = listOf("Live Music")),
        profile(id = "3", name = "Dimas Prasetyo", city = "Jakarta", bio = "Trail runner", interests = listOf("Hiking")),
    )

    private fun profile(
        id: String,
        name: String,
        city: String,
        bio: String,
        interests: List<String>,
    ) = UserProfile(
        id = id,
        name = name,
        age = 26,
        city = city,
        country = "Indonesia",
        distanceKm = 5.0,
        bio = bio,
        interests = interests,
    )

    @Test
    fun `matches by partial name case-insensitively`() {
        val results = UserSearchFilter.filter(users, "salsa")
        assertEquals(listOf("1"), results.map { it.id })
    }

    @Test
    fun `matches by city`() {
        val results = UserSearchFilter.filter(users, "bandung")
        assertEquals(listOf("2"), results.map { it.id })
    }

    @Test
    fun `matches by interest`() {
        val results = UserSearchFilter.filter(users, "hik")
        assertEquals(listOf("3"), results.map { it.id })
    }

    @Test
    fun `matches by bio content`() {
        val results = UserSearchFilter.filter(users, "coffee snob")
        assertEquals(listOf("1"), results.map { it.id })
    }

    @Test
    fun `blank query returns nothing`() {
        assertTrue(UserSearchFilter.filter(users, "   ").isEmpty())
    }

    @Test
    fun `no match returns empty list`() {
        assertTrue(UserSearchFilter.filter(users, "zzz").isEmpty())
    }
}
