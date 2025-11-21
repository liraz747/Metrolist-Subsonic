package com.metrolist.music.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.pages.ExplorePage
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.utils.reportException
import com.metrolist.subsonic.Subsonic
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    val database: MusicDatabase,
) : ViewModel() {
    val explorePage = MutableStateFlow<ExplorePage?>(null)

    private suspend fun load() {
        Subsonic.getAlbumList2("newest").onSuccess { albums ->
            explorePage.value = ExplorePage(
                newReleaseAlbums = albums,
                moodAndGenres = emptyList()
            )
        }.onFailure {
            reportException(it)
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            load()
        }
    }
}
