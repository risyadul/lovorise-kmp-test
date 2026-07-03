package com.lovorise.discover.core.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lovorise.discover.data.model.UserProfile
import com.lovorise.discover.data.repo.DiscoverRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Shared plumbing for Discover screens: one-shot snackbar events and the
 * connection-request interaction, so Home and Search cannot drift apart.
 */
abstract class BaseDiscoverViewModel(
    protected val repo: DiscoverRepository,
) : ViewModel() {

    private val _events = Channel<String>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    protected fun sendEvent(message: String) {
        viewModelScope.launch { _events.send(message) }
    }

    fun requestConnection(user: UserProfile) {
        repo.requestConnection(user.id)
        val requested = user.id in repo.session.value.requestedConnections
        sendEvent(
            if (requested) "Connection request sent to ${user.name.substringBefore(' ')}"
            else "Connection request withdrawn",
        )
    }

    /** Story id for a user's story bubble, if they have one. */
    fun storyIdFor(userId: String): String? =
        repo.stories().firstOrNull { it.user.id == userId }?.story?.id
}
