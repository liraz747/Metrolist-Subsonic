package com.metrolist.music.ui.screens.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.menu.SubsonicAlbumMenu
import com.metrolist.music.ui.menu.SubsonicArtistMenu
import com.metrolist.subsonic.Subsonic
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.currentCoroutineContext

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnlineSearchScreen(
    query: String,
    onQueryChange: (TextFieldValue) -> Unit,
    navController: NavController,
    onSearch: (String) -> Unit,
    onDismiss: () -> Unit,
    pureBlack: Boolean
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var searchResults by remember { mutableStateOf<Subsonic.SearchResult?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Debounced search to avoid stale scope exceptions when query rapidly changes
    LaunchedEffect(query) {
        if (query.length < 2) {
            searchResults = null
            errorMessage = null
            return@LaunchedEffect
        }
        isSearching = true
        errorMessage = null
        kotlinx.coroutines.delay(300)
        // Retry loop to handle early search before Subsonic initialization completes
        var attempt = 0
        val maxAttempts = 5
        while (attempt < maxAttempts && isActive) {
            attempt++
            try {
                val result = Subsonic.search3(query, songCount = 50, albumCount = 50, artistCount = 50)
                result.onSuccess { data ->
                    if (isActive) {
                        searchResults = data
                        if (data.songs.isEmpty() && data.albums.isEmpty() && data.artists.isEmpty()) {
                            errorMessage = "No results found"
                        }
                    }
                }.onFailure { e ->
                    if (isActive) {
                        // If initialization race, retry
                        if (e.message?.contains("not initialized", ignoreCase = true) == true && attempt < maxAttempts) {
                            kotlinx.coroutines.delay(200)
                        } else {
                            errorMessage = e.message
                            attempt = maxAttempts // abort further retries
                        }
                    }
                }
                break // break after successful request (Result created even on failure path)
            } catch (e: Exception) {
                if (isActive) {
                    if (e.message?.contains("not initialized", ignoreCase = true) == true && attempt < maxAttempts) {
                        kotlinx.coroutines.delay(200)
                        continue
                    }
                    errorMessage = e.message
                    break
                }
            }
        }
        if (isActive) {
            isSearching = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (query.isNotEmpty()) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Songs") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Albums") }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("Artists") }
                )
            }
        }

        when {
            isSearching -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            errorMessage != null -> Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Error: $errorMessage", color = MaterialTheme.colorScheme.error)
            }
            query.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Search for songs, albums, or artists")
            }
            searchResults == null -> Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Type at least 2 characters to search")
            }
            else -> {
                val results = searchResults!!
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    when (selectedTabIndex) {
                        0 -> items(results.songs) { song ->
                            SongSearchItem(
                                song = song,
                                onClick = {
                                    playerConnection.playQueue(ListQueue(items = listOf(song.toMediaItem())))
                                    onDismiss()
                                }
                            )
                        }
                        1 -> items(results.albums) { album ->
                            AlbumSearchItem(
                                album = album,
                                onClick = {
                                    onDismiss()
                                    navController.navigate("album/${album.id}")
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        SubsonicAlbumMenu(
                                            albumItem = album,
                                            navController = navController,
                                            onDismiss = menuState::dismiss
                                        )
                                    }
                                }
                            )
                        }
                        2 -> items(results.artists) { artist ->
                            ArtistSearchItem(
                                artist = artist,
                                onClick = {
                                    onDismiss()
                                    navController.navigate("artist/${artist.id}")
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        SubsonicArtistMenu(
                                            artist = artist,
                                            onDismiss = menuState::dismiss
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SongSearchItem(
    song: SongItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.thumbnail,
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1
            )
            Text(
                text = song.artists.joinToString { it.name },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumSearchItem(
    album: AlbumItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = album.thumbnail,
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1
            )
            Text(
                text = album.artists?.joinToString { it.name } ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Icon(
            painter = painterResource(R.drawable.album),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ArtistSearchItem(
    artist: ArtistItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = artist.thumbnail,
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(28.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artist.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1
            )
        }
        Icon(
            painter = painterResource(R.drawable.artist),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
