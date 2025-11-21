package com.metrolist.subsonic

import org.junit.Test
import org.junit.Assert.assertTrue
import kotlinx.coroutines.runBlocking

class SubsonicSmokeTest {
    private fun loadCredentials(): SubsonicCredentials? {
        val url = System.getenv("SUBSONIC_URL")
        val user = System.getenv("SUBSONIC_USERNAME")
        val pass = System.getenv("SUBSONIC_PASSWORD")
        val basePath = System.getenv("SUBSONIC_BASE_PATH")
        return if (!url.isNullOrBlank() && !user.isNullOrBlank() && !pass.isNullOrBlank()) {
            SubsonicCredentials(serverUrl = url, username = user, password = pass, basePath = basePath)
        } else null
    }

    @Test
    fun ping() = runBlocking {
        val credentials = loadCredentials() ?: return@runBlocking // skip if missing
        Subsonic.initialize(credentials)
        val result = Subsonic.ping()
        if (!result.isSuccess) {
            println("Ping error: ${result.exceptionOrNull()}")
            return@runBlocking
        }
        println("Ping success")
    }

    @Test
    fun pingAlternateBasePaths() = runBlocking {
        val credentials = loadCredentials() ?: return@runBlocking
        val candidates = listOf(null, "subsonic", "api", "hifi")
        candidates.forEach { path ->
            val creds = credentials.copy(basePath = path)
            Subsonic.initialize(creds)
            val r = Subsonic.ping()
            println("Ping basePath='${path ?: "<none>"}' success=${r.isSuccess}")
        }
    }

    @Test
    fun starred() = runBlocking {
        val credentials = loadCredentials() ?: return@runBlocking // skip
        Subsonic.initialize(credentials)
        val result = Subsonic.getStarred2()
        if (!result.isSuccess) {
            println("getStarred2 error: ${result.exceptionOrNull()}")
            return@runBlocking
        }
        val data = result.getOrNull()!!
        println("Starred: songs=${data.songs.size} albums=${data.albums.size} artists=${data.artists.size}")
    }

    @Test
    fun playlists() = runBlocking {
        val credentials = loadCredentials() ?: return@runBlocking // skip
        Subsonic.initialize(credentials)
        val result = Subsonic.getPlaylists()
        if (!result.isSuccess) {
            println("getPlaylists error: ${result.exceptionOrNull()}")
            return@runBlocking
        }
        val playlists = result.getOrNull()!!
        println("Playlists count=${playlists.size}")
        if (playlists.isNotEmpty()) {
            val first = playlists.first()
            val page = Subsonic.getPlaylist(first.id)
            if (!page.isSuccess) {
                println("getPlaylist error: ${page.exceptionOrNull()}")
                return@runBlocking
            }
            println("Playlist '${first.title}' songs=${page.getOrNull()!!.songs.size}")
        }
    }
}
