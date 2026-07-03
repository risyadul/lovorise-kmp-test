package com.lovorise.discover.feature.story

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lovorise.discover.AppGraph
import com.lovorise.discover.data.model.StoryUi
import com.lovorise.discover.data.model.UserProfile
import com.lovorise.discover.data.repo.DiscoverRepository
import com.lovorise.discover.data.repo.SessionState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class StoryViewerViewModel(
    private val repo: DiscoverRepository,
    /** Mirrors the row the user tapped: Connections stories stay in context. */
    private val connectionsOnly: Boolean,
) : ViewModel() {

    /**
     * Snapshot of the stories row ordering taken when the viewer opens, so
     * marking stories as seen doesn't reshuffle pager pages mid-session.
     */
    var stories: List<StoryUi> by mutableStateOf(emptyList())
        private set

    var isLoaded: Boolean by mutableStateOf(false)
        private set

    /** Small in-viewer confirmation (reactions, requests). */
    var transientMessage: String? by mutableStateOf(null)
        private set

    val session: StateFlow<SessionState> = repo.session

    init {
        viewModelScope.launch {
            try {
                repo.warmUp()
                stories = repo.stories(connectionsOnly = connectionsOnly)
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                stories = emptyList()
            } finally {
                isLoaded = true
            }
        }
    }

    fun markSeen(storyId: String) = repo.markStorySeen(storyId)

    fun react(storyId: String, emoji: String) {
        repo.reactToStory(storyId, emoji)
        transientMessage = "Reaction sent $emoji"
    }

    fun toggleSave(storyId: String) {
        repo.toggleStorySave(storyId)
        transientMessage = if (storyId in repo.session.value.savedStories) {
            "Story saved to your collection"
        } else {
            "Removed from saved"
        }
    }

    fun requestConnection(user: UserProfile) {
        repo.requestConnection(user.id)
        transientMessage = if (user.id in repo.session.value.requestedConnections) {
            "Connection request sent"
        } else {
            "Connection request withdrawn"
        }
    }

    fun sendMessageRequest(user: UserProfile, message: String) {
        repo.sendMessageRequest(user.id, message)
        transientMessage = "Message request sent to ${user.name.substringBefore(' ')}"
    }

    fun clearTransientMessage() {
        transientMessage = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val handle = createSavedStateHandle()
                StoryViewerViewModel(
                    repo = AppGraph.repository,
                    connectionsOnly = handle.get<Boolean>("connectionsOnly") ?: false,
                )
            }
        }
    }
}
