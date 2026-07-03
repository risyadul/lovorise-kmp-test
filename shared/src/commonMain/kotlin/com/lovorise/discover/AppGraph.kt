package com.lovorise.discover

import com.lovorise.discover.data.repo.DiscoverRepository
import com.lovorise.discover.data.source.MockApi

/**
 * Lightweight manual DI container shared by Android and iOS — a full
 * framework is overkill for a single repository.
 */
object AppGraph {
    val repository: DiscoverRepository by lazy { DiscoverRepository(api = MockApi()) }
}
