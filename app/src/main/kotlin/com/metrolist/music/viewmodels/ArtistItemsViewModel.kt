package com.metrolist.music.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.models.ItemsPage
import com.metrolist.subsonic.Subsonic
import com.metrolist.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArtistItemsViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val artistId = savedStateHandle.get<String>("artistId")!!

    val title = MutableStateFlow("")
    val itemsPage = MutableStateFlow<ItemsPage?>(null)

    init {
        viewModelScope.launch {
            Subsonic.getArtist(artistId).onSuccess { artistPage ->
                title.value = artistPage.artist.title
                itemsPage.value = ItemsPage(
                    items = artistPage.albums,
                    continuation = null,
                )
            }.onFailure {
                reportException(it)
            }
        }
    }

    fun loadMore() {
        // Subsonic API doesn't support continuation for artist items
    }
}
