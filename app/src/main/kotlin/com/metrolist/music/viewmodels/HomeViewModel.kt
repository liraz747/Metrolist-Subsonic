package com.metrolist.music.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.YTItem
import com.metrolist.innertube.models.filterExplicit
import com.metrolist.innertube.pages.ExplorePage
import com.metrolist.innertube.pages.HomePage
import com.metrolist.subsonic.Subsonic
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.constants.QuickPicks
import com.metrolist.music.constants.QuickPicksKey
import com.metrolist.music.constants.YtmSyncKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.Album
import com.metrolist.music.db.entities.LocalItem
import com.metrolist.music.db.entities.Song
import com.metrolist.music.extensions.toEnum
import com.metrolist.music.models.SimilarRecommendation
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import com.metrolist.music.utils.reportException
import com.metrolist.music.utils.SyncUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    val database: MusicDatabase,
    val syncUtils: SyncUtils,
) : ViewModel() {
    val isRefreshing = MutableStateFlow(false)
    val isLoading = MutableStateFlow(false)

    private val quickPicksEnum = context.dataStore.data.map {
        it[QuickPicksKey].toEnum(QuickPicks.QUICK_PICKS)
    }.distinctUntilChanged()

    val quickPicks = MutableStateFlow<List<Song>?>(null)
    val forgottenFavorites = MutableStateFlow<List<Song>?>(null)
    val keepListening = MutableStateFlow<List<LocalItem>?>(null)
    val similarRecommendations = MutableStateFlow<List<SimilarRecommendation>?>(null)
    val accountPlaylists = MutableStateFlow<List<PlaylistItem>?>(null)
    val homePage = MutableStateFlow<HomePage?>(null)
    val explorePage = MutableStateFlow<ExplorePage?>(null)
    val selectedChip = MutableStateFlow<HomePage.Chip?>(null)
    private val previousHomePage = MutableStateFlow<HomePage?>(null)

    val allLocalItems = MutableStateFlow<List<LocalItem>>(emptyList())
    val allYtItems = MutableStateFlow<List<YTItem>>(emptyList())

    val accountName = MutableStateFlow("Guest")
    val accountImageUrl = MutableStateFlow<String?>(null)

    // Track last processed cookie to avoid unnecessary updates
    private var lastProcessedCookie: String? = null
    // Track if we're currently processing account data
    private var isProcessingAccountData = false

    private suspend fun getQuickPicks() {
        when (quickPicksEnum.first()) {
            QuickPicks.QUICK_PICKS -> quickPicks.value = database.quickPicks().first().shuffled().take(20)
            QuickPicks.LAST_LISTEN -> {
                val song = database.events().first().firstOrNull()?.song
                if (song != null && database.hasRelatedSongs(song.id)) {
                    quickPicks.value = database.getRelatedSongs(song.id).first().shuffled().take(20)
                }
            }
        }
    }

    private suspend fun load() {
        isLoading.value = true
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)

        getQuickPicks()
        forgottenFavorites.value = database.forgottenFavorites().first().shuffled().take(20)

        val fromTimeStamp = System.currentTimeMillis() - 86400000 * 7 * 2
        val keepListeningSongs = database.mostPlayedSongs(fromTimeStamp, limit = 15, offset = 5).first().shuffled().take(10)
        val keepListeningAlbums = database.mostPlayedAlbums(fromTimeStamp, limit = 8, offset = 2).first().filter { it.album.thumbnailUrl != null }.shuffled().take(5)
        val keepListeningArtists = database.mostPlayedArtists(fromTimeStamp).first().filter { it.artist.isYouTubeArtist && it.artist.thumbnailUrl != null }.shuffled().take(5)
        keepListening.value = (keepListeningSongs + keepListeningAlbums + keepListeningArtists).shuffled()

        // Get playlists from Subsonic (if endpoint supported)
        try {
            Subsonic.getPlaylists().onSuccess {
                accountPlaylists.value = it
            }.onFailure {
                // 404 means endpoint not supported, silently skip
                if (it.message?.contains("404") != true && it !is IllegalStateException) {
                    reportException(it)
                }
            }
        } catch (e: IllegalStateException) {
            // Subsonic not initialized, skip
        }

        // Simplified recommendations using Subsonic - parallelize network calls
        val artistRecommendations = try {
            coroutineScope {
                database.mostPlayedArtists(fromTimeStamp, limit = 10).first()
                    .shuffled().take(3)
                    .map { artist ->
                        async(Dispatchers.IO) {
                            try {
                                Subsonic.getArtist(artist.id).getOrNull()?.let { page ->
                                    val items = page.albums.filterExplicit(hideExplicit).shuffled()
                                    if (items.isNotEmpty()) {
                                        SimilarRecommendation(
                                            title = artist,
                                            items = items
                                        )
                                    } else null
                                }
                            } catch (e: IllegalStateException) {
                                null // Subsonic not initialized
                            }
                        }
                    }
                    .awaitAll()
                    .filterNotNull()
            }
        } catch (e: IllegalStateException) {
            emptyList() // Subsonic not initialized
        }

        similarRecommendations.value = artistRecommendations.shuffled()

        // Create home page using Subsonic getAlbumList2 - parallelize network calls
        val (newestAlbums, recentAlbums, randomAlbums) = try {
            coroutineScope {
                val newestDeferred = async(Dispatchers.IO) {
                    try {
                        Subsonic.getAlbumList2("newest", size = 20).getOrNull() ?: emptyList()
                    } catch (e: IllegalStateException) {
                        emptyList() // Subsonic not initialized
                    }
                }
                val recentDeferred = async(Dispatchers.IO) {
                    try {
                        Subsonic.getAlbumList2("recent", size = 20).getOrNull() ?: emptyList()
                    } catch (e: IllegalStateException) {
                        emptyList() // Subsonic not initialized
                    }
                }
                val randomDeferred = async(Dispatchers.IO) {
                    try {
                        Subsonic.getAlbumList2("random", size = 20).getOrNull() ?: emptyList()
                    } catch (e: IllegalStateException) {
                        emptyList() // Subsonic not initialized
                    }
                }
                Triple(newestDeferred.await(), recentDeferred.await(), randomDeferred.await())
            }
        } catch (e: IllegalStateException) {
            Triple(emptyList(), emptyList(), emptyList()) // Subsonic not initialized
        }
        
        val sections = mutableListOf<HomePage.Section>()
        if (newestAlbums.isNotEmpty()) {
            sections.add(
                HomePage.Section(
                    title = "Newest Albums",
                    label = null,
                    thumbnail = null,
                    endpoint = null,
                    items = newestAlbums.filterExplicit(hideExplicit)
                )
            )
        }
        if (recentAlbums.isNotEmpty()) {
            sections.add(
                HomePage.Section(
                    title = "Recently Played",
                    label = null,
                    thumbnail = null,
                    endpoint = null,
                    items = recentAlbums.filterExplicit(hideExplicit)
                )
            )
        }
        if (randomAlbums.isNotEmpty()) {
            sections.add(
                HomePage.Section(
                    title = "Random Albums",
                    label = null,
                    thumbnail = null,
                    endpoint = null,
                    items = randomAlbums.filterExplicit(hideExplicit)
                )
            )
        }
        
        homePage.value = HomePage(
            chips = null,
            sections = sections,
            continuation = null
        )

        // Explore page using newest albums
        explorePage.value = ExplorePage(
            newReleaseAlbums = newestAlbums.filterExplicit(hideExplicit),
            moodAndGenres = emptyList()
        )

        allLocalItems.value = (quickPicks.value.orEmpty() + forgottenFavorites.value.orEmpty() + keepListening.value.orEmpty())
            .filter { it is Song || it is Album }
        allYtItems.value = similarRecommendations.value?.flatMap { it.items }.orEmpty() +
                homePage.value?.sections?.flatMap { it.items }.orEmpty()

        isLoading.value = false
    }

    private val _isLoadingMore = MutableStateFlow(false)
    fun loadMoreYouTubeItems(continuation: String?) {
        // Subsonic doesn't support pagination for home page
        // This is a no-op
    }

    fun toggleChip(chip: HomePage.Chip?) {
        // Subsonic doesn't support chips/filters for home page
        // This is a no-op
    }

    fun refresh() {
        if (isRefreshing.value) return
        viewModelScope.launch(Dispatchers.IO) {
            isRefreshing.value = true
            load()
            isRefreshing.value = false
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.data
                .map { it[InnerTubeCookieKey] }
                .distinctUntilChanged()
                .first()
            
            load()

            val isSyncEnabled = context.dataStore.get(YtmSyncKey, true)
            if (isSyncEnabled) {
                syncUtils.runAllSyncs()
            }
        }

        // Subsonic doesn't have account info like YouTube
        // Set default account info
        accountName.value = "Subsonic User"
        accountImageUrl.value = null
    }
}
