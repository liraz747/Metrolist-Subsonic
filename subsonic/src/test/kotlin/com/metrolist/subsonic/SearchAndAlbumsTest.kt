package com.metrolist.subsonic

import kotlinx.coroutines.runBlocking
import org.junit.Test

class SearchAndAlbumsTest {
    @Test
    fun testSearch3() = runBlocking {
        val url = System.getenv("SUBSONIC_URL") ?: return@runBlocking
        val username = System.getenv("SUBSONIC_USERNAME") ?: return@runBlocking
        val password = System.getenv("SUBSONIC_PASSWORD") ?: return@runBlocking
        
        val credentials = SubsonicCredentials(
            serverUrl = url,
            username = username,
            password = password
        )
        
        Subsonic.initialize(credentials)
        
        println("\n=== Testing search3 ===")
        val searchResult = Subsonic.search3("love", 10, 10, 10)
        searchResult.onSuccess { result ->
            println("✓ Search successful!")
            println("  Songs: ${result.songs.size}")
            result.songs.take(3).forEach { println("    - ${it.title}") }
            println("  Albums: ${result.albums.size}")
            result.albums.take(3).forEach { println("    - ${it.title}") }
            println("  Artists: ${result.artists.size}")
            result.artists.take(3).forEach { println("    - ${it.title}") }
            
            assert(result.songs.isNotEmpty()) { "Expected songs but got none" }
            assert(result.albums.isNotEmpty()) { "Expected albums but got none" }
            assert(result.artists.isNotEmpty()) { "Expected artists but got none" }
        }.onFailure {
            println("✗ Search error: ${it.message}")
            it.printStackTrace()
            throw it
        }
        
        println("\n=== Testing getAlbumList2 ===")
        val albumsResult = Subsonic.getAlbumList2("newest", size = 10)
        albumsResult.onSuccess { albums ->
            println("✓ Albums retrieved successfully!")
            println("  Albums returned: ${albums.size}")
            albums.take(3).forEach { println("    - ${it.title}") }
            
            assert(albums.isNotEmpty()) { "Expected albums but got none" }
        }.onFailure {
            println("✗ Album list error: ${it.message}")
            it.printStackTrace()
            throw it
        }
    }
}
