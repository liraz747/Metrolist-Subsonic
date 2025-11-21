package com.metrolist.music.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.models.filterExplicit
import com.metrolist.innertube.pages.SearchSummary
import com.metrolist.innertube.pages.SearchSummaryPage
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.models.ItemsPage
import com.metrolist.subsonic.Subsonic
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import com.metrolist.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnlineSearchViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val query = savedStateHandle.get<String>("query")!!
    val filter = MutableStateFlow<String?>(null) // "song", "album", "artist", or null for all
    var summaryPage by mutableStateOf<SearchSummaryPage?>(null)
    val viewStateMap = mutableStateMapOf<String, ItemsPage?>()

    init {
        viewModelScope.launch {
            filter.collect { filterType ->
                if (filterType == null) {
                    if (summaryPage == null) {
                        // Use Subsonic search3 for general search
                        Subsonic
                            .search3(query)
                            .onSuccess { result ->
                                val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                                // Convert Subsonic SearchResult to SearchSummaryPage format
                                val summaries = mutableListOf<SearchSummary>()
                                
                                if (result.songs.isNotEmpty()) {
                                    summaries.add(
                                        SearchSummary(
                                            title = "Songs",
                                            items = result.songs.filterExplicit(hideExplicit)
                                        )
                                    )
                                }
                                if (result.albums.isNotEmpty()) {
                                    summaries.add(
                                        SearchSummary(
                                            title = "Albums",
                                            items = result.albums.filterExplicit(hideExplicit)
                                        )
                                    )
                                }
                                if (result.artists.isNotEmpty()) {
                                    summaries.add(
                                        SearchSummary(
                                            title = "Artists",
                                            items = result.artists.filterExplicit(hideExplicit)
                                        )
                                    )
                                }
                                
                                summaryPage = SearchSummaryPage(summaries = summaries)
                            }.onFailure {
                                reportException(it)
                            }
                    }
                } else {
                    if (viewStateMap[filterType] == null) {
                        // Filter by type: "song", "album", "artist"
                        Subsonic
                            .search3(
                                query = query,
                                songCount = if (filterType == "song") 50 else 0,
                                albumCount = if (filterType == "album") 50 else 0,
                                artistCount = if (filterType == "artist") 50 else 0
                            )
                            .onSuccess { result ->
                                val items = when (filterType) {
                                    "song" -> result.songs
                                    "album" -> result.albums
                                    "artist" -> result.artists
                                    else -> emptyList()
                                }
                                viewStateMap[filterType] =
                                    ItemsPage(
                                        items
                                            .distinctBy { it.id }
                                            .filterExplicit(
                                                context.dataStore.get(
                                                    HideExplicitKey,
                                                    false
                                                )
                                            ),
                                        null, // Subsonic doesn't support continuation for search
                                    )
                            }.onFailure {
                                reportException(it)
                            }
                    }
                }
            }
        }
    }

    fun loadMore() {
        // Subsonic search doesn't support pagination, so this is a no-op
    }
}
