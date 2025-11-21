package com.metrolist.music.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.utils.reportException
import com.metrolist.subsonic.Subsonic
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumViewModel
@Inject
constructor(
    database: MusicDatabase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val albumId = savedStateHandle.get<String>("albumId")!!
    val playlistId = MutableStateFlow("")
    val albumWithSongs =
        database
            .albumWithSongs(albumId)
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    var otherVersions = MutableStateFlow<List<AlbumItem>>(emptyList())

    init {
        viewModelScope.launch {
            val album = database.album(albumId).first()
            Subsonic
                .getAlbum(albumId)
                .onSuccess { subsonicPage ->
                    playlistId.value = subsonicPage.album.id // Use album ID as playlist ID
                    otherVersions.value = emptyList() // Subsonic doesn't have "other versions"
                    // Convert Subsonic AlbumPage to YouTube AlbumPage format for database
                    val ytAlbumPage = com.metrolist.innertube.pages.AlbumPage(
                        album = subsonicPage.album,
                        songs = subsonicPage.songs,
                        otherVersions = emptyList()
                    )
                    database.transaction {
                        if (album == null) {
                            insert(ytAlbumPage)
                        } else {
                            update(album.album, ytAlbumPage, album.artists)
                        }
                    }
                }.onFailure {
                    reportException(it)
                    if (it.message?.contains("NOT_FOUND") == true || it.message?.contains("404") == true) {
                        database.query {
                            album?.album?.let(::delete)
                        }
                    }
                }
        }
    }
}
