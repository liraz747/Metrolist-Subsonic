# Subsonic API Integration

This document describes the Subsonic API integration that has been added to Metrolist to connect to your OpenSubsonic-compatible web app.

## What Has Been Done

1. **Created `subsonic` module** - A new module similar to the `innertube` module that handles Subsonic API communication
2. **SubsonicClient** - Low-level API client that handles authentication and HTTP requests
3. **Subsonic data models** - Kotlin data classes matching the OpenSubsonic API schema
4. **Subsonic wrapper** - High-level wrapper (`Subsonic.kt`) that maps Subsonic responses to Metrolist's `YTItem` models

## Module Structure

```
subsonic/
├── build.gradle.kts
└── src/main/kotlin/com/metrolist/subsonic/
    ├── SubsonicClient.kt      # Low-level API client
    ├── Subsonic.kt            # High-level wrapper (similar to YouTube.kt)
    └── models/
        └── SubsonicModels.kt  # Data models for API responses
```

## Initialization

Before using Subsonic, you need to initialize it with your server credentials. This should be done in your Application class or a dependency injection module.

```kotlin
import com.metrolist.subsonic.Subsonic
import com.metrolist.subsonic.SubsonicCredentials

// Initialize Subsonic
Subsonic.initialize(
    SubsonicCredentials(
        serverUrl = "https://your-subsonic-server.com",
        username = "your-username",
        password = "your-password"  // Or use token/salt if available
    )
)
```

## Usage Examples

### Search

```kotlin
import com.metrolist.subsonic.Subsonic

// Search for songs, albums, and artists
val result = Subsonic.search3(
    query = "search term",
    songCount = 50,
    albumCount = 50,
    artistCount = 50
).getOrNull()

result?.let {
    val songs = it.songs      // List<SongItem>
    val albums = it.albums    // List<AlbumItem>
    val artists = it.artists   // List<ArtistItem>
}
```

### Get Album

```kotlin
val albumPage = Subsonic.getAlbum(albumId).getOrNull()
albumPage?.let {
    val album = it.album      // AlbumItem
    val songs = it.songs      // List<SongItem>
}
```

### Get Artist

```kotlin
val artistPage = Subsonic.getArtist(artistId).getOrNull()
artistPage?.let {
    val artist = it.artist    // ArtistItem
    val albums = it.albums    // List<AlbumItem>
}
```

### Get Playlist

```kotlin
val playlistPage = Subsonic.getPlaylist(playlistId).getOrNull()
playlistPage?.let {
    val playlist = it.playlist  // PlaylistItem
    val songs = it.songs         // List<SongItem>
}
```

### Get Stream URL

```kotlin
// Get streaming URL for a song
val streamUrl = Subsonic.getStreamUrl(
    id = songId,
    maxBitRate = 320,  // Optional
    format = "mp3"     // Optional
)
```

### Get Cover Art URL

```kotlin
val coverArtUrl = Subsonic.getCoverArtUrl(
    id = coverArtId,
    size = 500  // Optional
)
```

## Integration with ViewModels

To replace YouTube API calls with Subsonic, update your ViewModels. Here's an example for `OnlineSearchViewModel`:

### Before (YouTube):
```kotlin
YouTube.searchSummary(query)
    .onSuccess { summaryPage = it }
```

### After (Subsonic):
```kotlin
Subsonic.search3(query)
    .onSuccess { result ->
        // Convert SearchResult to SearchSummaryPage format
        // or update UI to use SearchResult directly
    }
```

## What Still Needs to Be Done

1. **Update ViewModels** - Replace YouTube API calls with Subsonic calls in:
   - `OnlineSearchViewModel.kt`
   - `OnlineSearchSuggestionViewModel.kt`
   - `AlbumViewModel.kt`
   - `ArtistViewModel.kt`
   - `HomeViewModel.kt`
   - `OnlinePlaylistViewModel.kt`
   - And other ViewModels that use YouTube API

2. **Update UI Components** - Some UI components may need updates to handle Subsonic data structures

3. **Player Integration** - Update the player to use Subsonic stream URLs instead of YouTube URLs:
   - `MusicService.kt`
   - `YouTubeQueue.kt`
   - `YTPlayerUtils.kt`

4. **Settings/Configuration** - Add UI for users to configure Subsonic server credentials

5. **Error Handling** - Add proper error handling for Subsonic API failures

6. **Authentication** - Implement token-based authentication persistence

## API Endpoints Supported

The following OpenSubsonic endpoints are currently supported:

- `ping.view` - Check server status
- `getMusicFolders.view` - Get music folders
- `getIndexes.view` - Get indexes
- `getArtists.view` - Get artists
- `getAlbum.view` - Get album details
- `getArtist.view` - Get artist details
- `search3.view` - Search for songs, albums, artists
- `getAlbumList2.view` - Get album lists
- `getPlaylists.view` - Get playlists
- `getPlaylist.view` - Get playlist details
- `getStarred2.view` - Get starred items
- `stream.view` - Stream audio
- `getCoverArt.view` - Get cover art

## Notes

- The Subsonic wrapper converts Subsonic API responses to Metrolist's `YTItem` models, so existing UI components should work with minimal changes
- Stream URLs are generated using `Subsonic.getStreamUrl()` - these can be used directly with ExoPlayer
- Cover art URLs are generated using `Subsonic.getCoverArtUrl()` - these can be used with Coil for image loading
- The implementation uses token-based authentication (MD5 hash) as per OpenSubsonic specification

## Testing

To test the integration:

1. Initialize Subsonic with your server credentials
2. Test basic operations like search, get album, get artist
3. Verify stream URLs work with ExoPlayer
4. Check that cover art loads correctly

## Troubleshooting

- **Authentication errors**: Verify server URL, username, and password are correct
- **Response parsing errors**: Check that your server returns valid OpenSubsonic JSON responses
- **Stream URL issues**: Ensure the server supports the `stream.view` endpoint
- **Cover art issues**: Verify cover art IDs are valid and the server supports `getCoverArt.view`

