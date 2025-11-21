package com.metrolist.subsonic.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

// Serializer that accepts either a numeric literal or a quoted string and always produces a String
object FlexibleStringIntSerializer : KSerializer<String?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FlexibleStringInt", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: String?) {
        if (value == null) encoder.encodeString("") else encoder.encodeString(value)
    }
    override fun deserialize(decoder: Decoder): String? {
        return try {
            // Attempt to get raw JSON element to handle numbers or strings uniformly
            val jsonDecoder = decoder as? JsonDecoder
            val element: JsonElement? = jsonDecoder?.decodeJsonElement()
            val primitive = element as? JsonPrimitive
            primitive?.content
        } catch (e: Exception) {
            // Fallback to standard string decode (will work for proper quoted strings)
            runCatching { decoder.decodeString() }.getOrNull()
        }
    }
}

// Serializer that accepts either a numeric literal or a quoted string and produces an Int?
object FlexibleIntStringSerializer : KSerializer<Int?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FlexibleIntString", PrimitiveKind.INT)
    override fun serialize(encoder: Encoder, value: Int?) {
        if (value == null) encoder.encodeInt(0) else encoder.encodeInt(value)
    }
    override fun deserialize(decoder: Decoder): Int? {
        return try {
            val jsonDecoder = decoder as? JsonDecoder
            val element = jsonDecoder?.decodeJsonElement() as? JsonPrimitive
            val raw = element?.content
            raw?.toIntOrNull()
        } catch (e: Exception) {
            runCatching { decoder.decodeInt() }.getOrNull()
        }
    }
}

@Serializable
data class SubsonicWrapper<T>(
    @SerialName("subsonic-response")
    val response: SubsonicResponse<T>
)

@Serializable
data class SubsonicResponse<T>(
    val status: String,
    val version: String? = null,
    val type: String? = null,
    val serverVersion: String? = null,
    val openSubsonic: Boolean? = null,
    val error: SubsonicError? = null
) {
    // The actual data fields will be at the same level as status
    // We'll use JsonObject to extract them dynamically
}

@Serializable
data class SubsonicError(
    val code: Int? = null,
    val message: String? = null
)

@Serializable
data class SubsonicStatus(
    val version: String? = null
)

@Serializable
data class MusicFolders(
    val musicFolder: List<MusicFolder>? = emptyList()
)

@Serializable
data class MusicFolder(
    val id: String,
    val name: String? = null
)

@Serializable
data class Indexes(
    val index: List<Index>? = emptyList(),
    val lastModified: Long? = null,
    val ignoredArticles: String? = null
)

@Serializable
data class Index(
    val name: String,
    val artist: List<ArtistID3>? = emptyList()
)

@Serializable
data class ArtistsID3(
    val index: List<Index>? = emptyList(),
    val ignoredArticles: String? = null
)

@Serializable
data class ArtistID3(
    val id: String,
    val name: String,
    val coverArt: String? = null,
    val artistImageUrl: String? = null,
    val albumCount: Int? = null,
    val starred: String? = null,
    val musicBrainzId: String? = null,
    val sortName: String? = null,
    val roles: List<String>? = null
)

@Serializable
data class AlbumID3(
    val id: String,
    val name: String,
    val version: String? = null,
    val artist: String? = null,
    @Serializable(with = FlexibleStringIntSerializer::class)
    val artistId: String? = null,
    val coverArt: String? = null,
    val songCount: Int,
    val duration: Int,
    val playCount: Int? = null,
    val created: String? = null,
    val starred: String? = null,
    @Serializable(with = FlexibleIntStringSerializer::class)
    val year: Int? = null,
    val genre: String? = null,
    val played: String? = null,
    val userRating: Int? = null,
    val recordLabels: List<RecordLabel>? = null,
    val musicBrainzId: String? = null,
    val genres: List<ItemGenre>? = null,
    val artists: List<ArtistID3>? = null,
    val displayArtist: String? = null,
    val releaseTypes: List<String>? = null,
    val moods: List<String>? = null,
    val sortName: String? = null,
    val originalReleaseDate: ItemDate? = null,
    val releaseDate: ItemDate? = null,
    val isCompilation: Boolean? = null,
    val explicitStatus: String? = null,
    val discTitles: List<DiscTitle>? = null
)

@Serializable
data class AlbumID3WithSongs(
    val id: String,
    val name: String,
    val version: String? = null,
    val artist: String? = null,
    @Serializable(with = FlexibleStringIntSerializer::class)
    val artistId: String? = null,
    val coverArt: String? = null,
    val songCount: Int,
    val duration: Int,
    val playCount: Int? = null,
    val created: String? = null,
    val starred: String? = null,
    @Serializable(with = FlexibleIntStringSerializer::class)
    val year: Int? = null,
    val genre: String? = null,
    val played: String? = null,
    val userRating: Int? = null,
    val recordLabels: List<RecordLabel>? = null,
    val musicBrainzId: String? = null,
    val genres: List<ItemGenre>? = null,
    val artists: List<ArtistID3>? = null,
    val displayArtist: String? = null,
    val releaseTypes: List<String>? = null,
    val moods: List<String>? = null,
    val sortName: String? = null,
    val originalReleaseDate: ItemDate? = null,
    val releaseDate: ItemDate? = null,
    val isCompilation: Boolean? = null,
    val explicitStatus: String? = null,
    val discTitles: List<DiscTitle>? = null,
    val song: List<Child>? = emptyList()
)

@Serializable
data class ArtistWithAlbumsID3(
    val id: String,
    val name: String,
    val coverArt: String? = null,
    val artistImageUrl: String? = null,
    val albumCount: Int? = null,
    val starred: String? = null,
    val musicBrainzId: String? = null,
    val sortName: String? = null,
    val roles: List<String>? = null,
    val album: List<AlbumID3>? = emptyList()
)

@Serializable
data class Child(
    val id: String,
    @Serializable(with = FlexibleStringIntSerializer::class)
    val parent: String? = null,
    val isDir: Boolean = false,
    val title: String,
    val album: String? = null,
    val artist: String? = null,
    val track: Int? = null,
    @Serializable(with = FlexibleIntStringSerializer::class)
    val year: Int? = null,
    val genre: String? = null,
    val coverArt: String? = null,
    val size: Long? = null,
    val contentType: String? = null,
    val suffix: String? = null,
    val transcodedContentType: String? = null,
    val transcodedSuffix: String? = null,
    val duration: Int? = null,
    val bitRate: Int? = null,
    val bitDepth: Int? = null,
    val samplingRate: Int? = null,
    val channelCount: Int? = null,
    val path: String? = null,
    val isVideo: Boolean? = null,
    val userRating: Int? = null,
    val averageRating: Double? = null,
    val playCount: Int? = null,
    val discNumber: Int? = null,
    val created: String? = null,
    val starred: String? = null,
    @Serializable(with = FlexibleStringIntSerializer::class)
    val albumId: String? = null,
    @Serializable(with = FlexibleStringIntSerializer::class)
    val artistId: String? = null,
    val type: String? = null,
    val mediaType: String? = null,
    val bookmarkPosition: Int? = null,
    val originalWidth: Int? = null,
    val originalHeight: Int? = null,
    val played: String? = null,
    val bpm: Int? = null,
    val comment: String? = null,
    val sortName: String? = null,
    val musicBrainzId: String? = null,
    val isrc: List<String>? = null,
    val genres: List<ItemGenre>? = null,
    val artists: List<ArtistID3>? = null,
    val displayArtist: String? = null,
    val albumArtists: List<ArtistID3>? = null,
    val displayAlbumArtist: String? = null,
    val contributors: List<Contributor>? = null,
    val displayComposer: String? = null,
    val moods: List<String>? = null,
    val replayGain: ReplayGain? = null,
    val explicitStatus: String? = null
)

@Serializable
data class SearchResult3(
    val artist: List<ArtistID3>? = emptyList(),
    val album: List<AlbumID3>? = emptyList(),
    val song: List<Child>? = emptyList()
)

@Serializable
data class AlbumList2(
    val album: List<AlbumID3>? = emptyList()
)

@Serializable
data class Playlists(
    val playlist: List<Playlist>? = emptyList()
)

@Serializable
data class Playlist(
    val id: String,
    val name: String,
    val comment: String? = null,
    val owner: String? = null,
    val `public`: Boolean? = null,
    val songCount: Int? = null,
    val duration: Int? = null,
    val created: String? = null,
    val changed: String? = null,
    val coverArt: String? = null
)

@Serializable
data class PlaylistWithSongs(
    val id: String,
    val name: String,
    val comment: String? = null,
    val owner: String? = null,
    val `public`: Boolean? = null,
    val songCount: Int? = null,
    val duration: Int? = null,
    val created: String? = null,
    val changed: String? = null,
    val coverArt: String? = null,
    val entry: List<Child>? = emptyList()
)

@Serializable
data class Starred2(
    val song: List<Child>? = emptyList(),
    val album: List<AlbumID3>? = emptyList(),
    val artist: List<ArtistID3>? = emptyList()
)

@Serializable
data class RecordLabel(
    val name: String? = null
)

@Serializable
data class ItemGenre(
    val name: String? = null
)

@Serializable
data class DiscTitle(
    val discNumber: Int? = null,
    val title: String? = null
)

@Serializable
data class ItemDate(
    val year: Int? = null,
    val month: Int? = null,
    val day: Int? = null
)

@Serializable
data class Contributor(
    val name: String? = null,
    val role: String? = null
)

@Serializable
data class ReplayGain(
    val trackGain: Double? = null,
    val trackPeak: Double? = null,
    val albumGain: Double? = null,
    val albumPeak: Double? = null
)

