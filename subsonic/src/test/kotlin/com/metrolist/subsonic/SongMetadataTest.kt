package com.metrolist.subsonic

import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class SongMetadataTest {
    
    @Before
    fun setup() {
        // Initialize with test credentials
        Subsonic.initialize(
            SubsonicCredentials(
                serverUrl = "https://demo.navidrome.org",
                username = "demo",
                password = "demo"
            )
        )
    }
    
    @Test
    fun `test get song metadata with bitrate and sample rate`() = runBlocking {
        println("\n=== Testing Song Metadata Retrieval ===")
        
        // Get albums first, then get songs from an album
        val albumsResult = Subsonic.getAlbumList2("random", size = 1)
        val albums = albumsResult.getOrNull() ?: emptyList()
        
        println("Found ${albums.size} albums")
        
        if (albums.isNotEmpty()) {
            val firstAlbum = albums.first()
            println("\nFirst album:")
            println("  ID: ${firstAlbum.id}")
            println("  Title: ${firstAlbum.title}")
            
            // Get album songs
            println("\nFetching album songs...")
            val albumResult = Subsonic.getAlbum(firstAlbum.id)
            val album = albumResult.getOrNull()
                
            if (album != null && album.songs.isNotEmpty()) {
                println("Album: ${album.album.title}")
                println("Songs in album: ${album.songs.size}")
                    
                val firstSong = album.songs.first()
                println("\nFirst song: ${firstSong.title}")
            
                // Test getSongMetadata method
                println("\nFetching song metadata (direct getSong)...")
                val metadataResult = Subsonic.getSongMetadata(firstSong.id)
                val metadata = metadataResult.getOrNull()
            
                if (metadata != null) {
                    println("\nSong Metadata:")
                    println("  Bit Rate: ${metadata.bitRate} kbps")
                    println("  Sampling Rate: ${metadata.samplingRate} Hz")
                    println("  Bit Depth: ${metadata.bitDepth} bits")
                    println("  Channel Count: ${metadata.channelCount}")
                    println("  File Size: ${metadata.size} bytes")
                    println("  Content Type: ${metadata.contentType}")
                    println("  Suffix: ${metadata.suffix}")
                } else {
                    println("\nFailed to get metadata: ${metadataResult.exceptionOrNull()?.message}")
                }
            } else {
                println("\nNo songs in album or failed to get album")
            }
        }
        
        println("\n=== Test Complete ===\n")
    }
}
