package com.metrolist.subsonic

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Integration tests for Subsonic API wrapper.
 * These tests require the following environment variables:
 *  SUBSONIC_URL, SUBSONIC_USERNAME, SUBSONIC_PASSWORD
 */
class SubsonicApiIntegrationTest {

    private fun initOrSkip(): Boolean {
        val url = System.getenv("SUBSONIC_URL") ?: return false
        val username = System.getenv("SUBSONIC_USERNAME") ?: return false
        val password = System.getenv("SUBSONIC_PASSWORD") ?: return false
        Subsonic.initialize(
            SubsonicCredentials(
                serverUrl = url,
                username = username,
                password = password
            )
        )
        return true
    }

    @Test
    fun songOnlySearchReturnsSongs() = runBlocking {
        if (!initOrSkip()) return@runBlocking
        val result = Subsonic.search3("love", songCount = 5, albumCount = 0, artistCount = 0)
        result.onFailure { throw it }
        val data = result.getOrThrow()
        assertTrue("Expected at least 1 song", data.songs.isNotEmpty())
        data.songs.forEach { song ->
            assertTrue("Song id blank", song.id.isNotBlank())
            assertTrue("Song title blank", song.title.isNotBlank())
        }
    }

    @Test
    fun albumOnlySearchReturnsAlbums() = runBlocking {
        if (!initOrSkip()) return@runBlocking
        val result = Subsonic.search3("love", songCount = 0, albumCount = 5, artistCount = 0)
        result.onFailure { throw it }
        val data = result.getOrThrow()
        assertTrue("Expected at least 1 album", data.albums.isNotEmpty())
        data.albums.forEach { album ->
            assertTrue("Album id blank", album.id.isNotBlank())
            assertTrue("Album title blank", album.title.isNotBlank())
        }
    }

    @Test
    fun artistOnlySearchReturnsArtists() = runBlocking {
        if (!initOrSkip()) return@runBlocking
        val result = Subsonic.search3("love", songCount = 0, albumCount = 0, artistCount = 5)
        result.onFailure { throw it }
        val data = result.getOrThrow()
        assertTrue("Expected at least 1 artist", data.artists.isNotEmpty())
        data.artists.forEach { artist ->
            assertTrue("Artist id blank", artist.id.isNotBlank())
            assertTrue("Artist title blank", artist.title.isNotBlank())
        }
    }

    @Test
    fun albumListNewestReturnsAlbums() = runBlocking {
        if (!initOrSkip()) return@runBlocking
        val result = Subsonic.getAlbumList2("newest", size = 10)
        result.onFailure { throw it }
        val albums = result.getOrThrow()
        assertTrue("Expected albums from newest list", albums.isNotEmpty())
        albums.forEach { album ->
            assertTrue("Newest album id blank", album.id.isNotBlank())
            assertTrue("Newest album title blank", album.title.isNotBlank())
        }
    }

    @Test
    fun albumDetailReturnsSongs() = runBlocking {
        if (!initOrSkip()) return@runBlocking
        val albumsResult = Subsonic.getAlbumList2("newest", size = 5)
        albumsResult.onFailure { throw it }
        val albums = albumsResult.getOrThrow()
        assertTrue("Need at least one album to test details", albums.isNotEmpty())
        val firstAlbumId = albums.first().id
        val albumPageResult = Subsonic.getAlbum(firstAlbumId)
        albumPageResult.onFailure { throw it }
        val albumPage = albumPageResult.getOrThrow()
        assertNotNull("Album page album null", albumPage.album)
        assertTrue("Album page songs empty", albumPage.songs.isNotEmpty())
    }
}
