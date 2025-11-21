package com.metrolist.subsonic

import kotlinx.coroutines.runBlocking
import org.junit.Test

class UserServerMetadataTest {
    @Test
    fun `fetch metadata from user server`() = runBlocking {
        println("\n=== User Server Metadata Retrieval ===")
        Subsonic.initialize(
            SubsonicCredentials(
                serverUrl = "https://api.401658.xyz",
                username = "liraz747",
                password = "HOTmail747"
            )
        )

        // Get a random album then first song
        val albums = Subsonic.getAlbumList2("random", size = 1).getOrNull()
        if (albums.isNullOrEmpty()) {
            println("No albums returned.")
            return@runBlocking
        }
        val album = albums.first()
        println("Album: ${album.title} (id=${album.id})")

        val albumPage = Subsonic.getAlbum(album.id).getOrNull()
        if (albumPage == null || albumPage.songs.isEmpty()) {
            println("No songs in album page.")
            return@runBlocking
        }
        val song = albumPage.songs.first()
        println("Song: ${song.title} (id=${song.id})")

        val meta = Subsonic.getSongMetadata(song.id).getOrNull()
        if (meta == null) {
            println("Song metadata missing, fallback failed.")
        } else {
            println("Final Metadata: bitrate=${meta.bitRate}kbps sampleRate=${meta.samplingRate}Hz size=${meta.size} contentType=${meta.contentType} suffix=${meta.suffix}")
        }
        println("=== End User Server Metadata Retrieval ===\n")
    }
}
