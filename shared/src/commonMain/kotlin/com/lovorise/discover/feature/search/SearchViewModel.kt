package com.lovorise.discover.feature.search

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lovorise.discover.AppGraph
import com.lovorise.discover.core.ui.BaseDiscoverViewModel
import com.lovorise.discover.data.model.ScoredProfile
import com.lovorise.discover.data.model.UserProfile
import com.lovorise.discover.data.repo.DiscoverRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val recentSearches: List<String> = emptyList(),
    val suggested: List<ScoredProfile> = emptyList(),
    val results: List<UserProfile> = emptyList(),
    val requestedConnections: Set<String> = emptySet(),
    val connectedUsers: Set<String> = emptySet(),
)

class SearchViewModel(repo: DiscoverRepository) : BaseDiscoverViewModel(repo) {

    private val query = MutableStateFlow("")

    val uiState: StateFlow<SearchUiState> =
        combine(query, repo.session) { query, session ->
            SearchUiState(
                query = query,
                recentSearches = session.recentSearches,
                suggested = repo.recommendedProfiles().take(6),
                results = repo.searchUsers(query),
                requestedConnections = session.requestedConnections,
                connectedUsers = session.connectedUsers,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    init {
        // Search can be opened before Home finishes loading; make sure the
        // catalogue is available for local filtering.
        viewModelScope.launch {
            try {
                repo.warmUp()
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                sendEvent("Couldn't load people right now")
            }
        }
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun submitSearch() {
        repo.commitRecentSearch(query.value)
    }

    fun selectRecent(term: String) {
        query.value = term
    }

    fun removeRecent(term: String) = repo.removeRecentSearch(term)

    fun clearRecents() = repo.clearRecentSearches()

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SearchViewModel(AppGraph.repository)
            }
        }
    }
}
