package com.metrolist.subsonic

import com.metrolist.subsonic.models.*
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.net.Proxy
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class SubsonicCredentials(
    val serverUrl: String,
    val username: String,
    val password: String? = null,
    val token: String? = null,
    val salt: String? = null,
    val basePath: String? = null
)

class SubsonicClient(private val credentials: SubsonicCredentials) {
    var httpClient = createClient()
        private set
    private val clientName = "Metrolist"
    private val apiVersion = "1.16.1"

    var proxy: Proxy? = null
        set(value) {
            field = value
            httpClient.close()
            httpClient = createClient()
        }

    @OptIn(ExperimentalSerializationApi::class)
    internal fun createClient() = HttpClient(OkHttp) {
        expectSuccess = true

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                explicitNulls = false
                encodeDefaults = true
            })
        }

        install(ContentEncoding) {
            gzip(0.9F)
            deflate(0.8F)
        }

        engine {
            config {
                connectTimeout(10, TimeUnit.SECONDS)
                readTimeout(30, TimeUnit.SECONDS)
                writeTimeout(30, TimeUnit.SECONDS)
            }
            proxy?.let {
                proxy = this@SubsonicClient.proxy
            }
        }
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun generateSalt(): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_"
        return (1..8).map { chars.random() }.joinToString("")
    }

    internal fun getAuthParams(): Map<String, String> {
        val params = mutableMapOf<String, String>(
            "u" to credentials.username,
            "v" to apiVersion,
            "c" to clientName,
            "f" to "json"
        )

        when {
            credentials.token != null && credentials.salt != null -> {
                params["t"] = credentials.token
                params["s"] = credentials.salt
            }
            credentials.password != null -> {
                val salt = credentials.salt ?: generateSalt()
                val token = md5(credentials.password + salt)
                params["t"] = token
                params["s"] = salt
            }
        }

        return params
    }

    fun buildUrl(endpoint: String, queryParams: Map<String, String> = emptyMap()): String {
        val root = credentials.serverUrl.removeSuffix("/")
        val pathSegment = credentials.basePath?.takeIf { it.isNotBlank() }?.trim('/')
        val finalBase = if (pathSegment != null) "$root/$pathSegment" else root
        val authParams = getAuthParams()
        val allParams = authParams + queryParams

        val url = URLBuilder("$finalBase/rest/$endpoint")
        allParams.forEach { (key, value) -> url.parameters.append(key, value) }
        return url.buildString()
    }

    suspend inline fun <reified T> request(endpoint: String, params: Map<String, String> = emptyMap()): T {
        val url = this.buildUrl(endpoint, params)
        val debug = System.getenv("SUBSONIC_DEBUG") == "1"
        val response = try {
            this.httpClient.get(url)
        } catch (e: Exception) {
            if (debug) println("[Subsonic] Request failed url=$url exception=${e.message}")
            throw e
        }
        if (debug) println("[Subsonic] GET ${response.status.value} url=$url")
        val jsonString = response.bodyAsText()
        val json = Json { 
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = true
        }
        
        val jsonObject = json.parseToJsonElement(jsonString).jsonObject
        val subsonicResponse = jsonObject["subsonic-response"]?.jsonObject
            ?: throw Exception("Invalid response format: missing subsonic-response")
        if (debug) println("[Subsonic] Body keys=${jsonObject.keys} statusField=${subsonicResponse["status"]}")
        
        val status = subsonicResponse["status"]?.jsonPrimitive?.contentOrNull
            ?: throw Exception("Missing status in response")
        
        if (status == "failed") {
            val error = subsonicResponse["error"]?.jsonObject
            val message = error?.get("message")?.jsonPrimitive?.contentOrNull ?: "Unknown error"
            val code = error?.get("code")?.jsonPrimitive?.contentOrNull
            throw Exception("Subsonic error (code: $code): $message")
        }
        
        // Extract the data field (which varies by endpoint)
        // The actual data is in a field like "searchResult3", "albumList2", etc.
        // Find the first field that isn't status/version/type/serverVersion/openSubsonic/error
        val dataFields = subsonicResponse.filterKeys { 
            it !in listOf("status", "version", "type", "serverVersion", "openSubsonic", "error")
        }
        
        // If there's exactly one data field, extract its value directly
        // Otherwise, return the whole filtered object (for types like SubsonicStatus with no data field)
        val dataJson = if (dataFields.size == 1) {
            dataFields.values.first()
        } else {
            JsonObject(dataFields)
        }
        
        @Suppress("UNCHECKED_CAST")
        return json.decodeFromJsonElement(serializer<T>(), dataJson)
    }

    suspend fun ping() = request<SubsonicStatus>("ping.view")
    
    // Helper to get client for cover art URLs
    fun getClientForUrls(): SubsonicClient = this

    suspend fun getMusicFolders() = request<MusicFolders>("getMusicFolders.view")

    suspend fun getIndexes(musicFolderId: String? = null, ifModifiedSince: Long? = null) = request<Indexes>(
        "getIndexes.view",
        buildMap {
            musicFolderId?.let { put("musicFolderId", it) }
            ifModifiedSince?.let { put("ifModifiedSince", it.toString()) }
        }
    )

    suspend fun getArtists(musicFolderId: String? = null) = request<ArtistsID3>(
        "getArtists.view",
        buildMap {
            musicFolderId?.let { put("musicFolderId", it) }
        }
    )

    suspend fun getAlbum(id: String) = request<AlbumID3WithSongs>("getAlbum.view", mapOf("id" to id))

    suspend fun getArtist(id: String) = request<ArtistWithAlbumsID3>("getArtist.view", mapOf("id" to id))

    suspend fun search3(
        query: String,
        songCount: Int = 50,
        albumCount: Int = 50,
        artistCount: Int = 50
    ) = request<SearchResult3>(
        "search3.view",
        mapOf(
            "query" to query,
            "songCount" to songCount.toString(),
            "albumCount" to albumCount.toString(),
            "artistCount" to artistCount.toString()
        )
    )

    suspend fun getAlbumList2(
        type: String,
        size: Int = 20,
        offset: Int = 0,
        musicFolderId: String? = null
    ) = request<AlbumList2>(
        "getAlbumList2.view",
        buildMap {
            put("type", type)
            put("size", size.toString())
            put("offset", offset.toString())
            musicFolderId?.let { put("musicFolderId", it) }
        }
    )

    // Fetch single song (Child) with full metadata by id
    suspend fun getSong(id: String) = request<Child>(
        "getSong.view",
        mapOf("id" to id)
    )

    fun getStreamUrl(id: String, maxBitRate: Int? = null, format: String? = null): String {
        val params = buildMap {
            put("id", id)
            maxBitRate?.let { put("maxBitRate", it.toString()) }
            format?.let { put("format", it) }
        }
        return buildUrl("stream.view", params)
    }

    fun getCoverArtUrl(id: String, size: Int? = null): String {
        val params = buildMap {
            put("id", id)
            size?.let { put("size", it.toString()) }
        }
        return buildUrl("getCoverArt.view", params)
    }

    suspend fun getPlaylists() = request<Playlists>("getPlaylists.view")

    suspend fun getPlaylist(id: String) = request<PlaylistWithSongs>("getPlaylist.view", mapOf("id" to id))

    suspend fun getStarred2() = request<Starred2>("getStarred2.view")

    suspend fun star(id: String? = null, albumId: String? = null, artistId: String? = null) {
        val params = buildMap<String, String> {
            id?.let { put("id", it) }
            albumId?.let { put("albumId", it) }
            artistId?.let { put("artistId", it) }
        }
        request<SubsonicStatus>("star.view", params)
    }

    suspend fun unstar(id: String? = null, albumId: String? = null, artistId: String? = null) {
        val params = buildMap<String, String> {
            id?.let { put("id", it) }
            albumId?.let { put("albumId", it) }
            artistId?.let { put("artistId", it) }
        }
        request<SubsonicStatus>("unstar.view", params)
    }
}

