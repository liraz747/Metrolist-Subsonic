package com.metrolist.subsonic

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class StreamingAndCoverArtTest {
    private fun initOrSkip(): Boolean {
        val url = System.getenv("SUBSONIC_URL") ?: return false
        val username = System.getenv("SUBSONIC_USERNAME") ?: return false
        val password = System.getenv("SUBSONIC_PASSWORD") ?: return false
        Subsonic.initialize(SubsonicCredentials(url, username, password))
        return true
    }

    @Test
    fun streamUrlReturnsAudio() = runBlocking {
        if (!initOrSkip()) return@runBlocking
        val search = Subsonic.search3("love", songCount = 1, albumCount = 0, artistCount = 0).getOrThrow()
        val song = search.songs.first()
        val streamUrl = Subsonic.getStreamUrl(song.id, format = "mp3")
        val client = HttpClient()
        val headResponse: HttpResponse = client.get(streamUrl) // some servers may not honor HEAD; GET small data
        assertEquals(HttpStatusCode.OK, headResponse.status)
        val contentType = headResponse.headers[HttpHeaders.ContentType] ?: ""
        assertTrue("Expected audio content type, got $contentType", contentType.startsWith("audio"))
    }

    @Test
    fun coverArtReturnsBytes() = runBlocking {
        if (!initOrSkip()) return@runBlocking
        val albums = Subsonic.getAlbumList2("newest", size = 1).getOrThrow()
        val album = albums.first()
        val coverArt = album.thumbnail
        assertNotNull("Thumbnail null", coverArt)
        val client = HttpClient()
        val response: HttpResponse = client.get(coverArt!!)
        assertEquals(HttpStatusCode.OK, response.status)
        val bytes: ByteArray = response.body()
        assertTrue("Cover art bytes empty", bytes.isNotEmpty())
    }
}
