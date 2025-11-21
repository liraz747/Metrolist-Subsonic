package com.metrolist.music.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.utils.reportException
import com.metrolist.innertube.models.YTItem
import com.metrolist.subsonic.Subsonic
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BrowseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val browseId: String? = savedStateHandle.get<String>("browseId")

    val items = MutableStateFlow<List<YTItem>>(emptyList())
    val title = MutableStateFlow<String?>("")

    init {
        viewModelScope.launch {
            browseId?.let {
                when (it) {
                    "albums" -> {
                        title.value = "Albums"
                        Subsonic.getAlbumList2("newest").onSuccess { result ->
                            items.value = result
                        }.onFailure {
                            reportException(it)
                        }
                    }
                    "artists" -> {
                        title.value = "Artists"
                        Subsonic.getArtists().onSuccess { result ->
                            items.value = result
                        }.onFailure {
                            reportException(it)
                        }
                    }
                    "playlists" -> {
                        title.value = "Playlists"
                        Subsonic.getPlaylists().onSuccess { result ->
                            items.value = result
                        }.onFailure {
                            reportException(it)
                        }
                    }
                }
            }
        }
    }
}
