package com.metrolist.music.ui.utils

import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.util.UnstableApi
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.MediaInfo
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.DarkModeKey
import com.metrolist.music.constants.PureBlackKey
import com.metrolist.music.db.entities.FormatEntity
import com.metrolist.music.db.entities.Song
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.MenuState
import com.metrolist.music.ui.component.shimmer.ShimmerHost
import com.metrolist.music.ui.component.shimmer.TextPlaceholder
import com.metrolist.music.ui.screens.settings.DarkMode
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import android.content.ClipData
import android.content.ClipboardManager

@Composable
fun ShowMediaInfo(videoId: String) {
    if (videoId.isBlank() || videoId.isEmpty()) return

    val windowInsets = WindowInsets.systemBars

    var info by remember {
        mutableStateOf<MediaInfo?>(null)
    }

    val database = LocalDatabase.current
    var song by remember { mutableStateOf<Song?>(null) }
    var fallbackMetadata by remember { mutableStateOf<com.metrolist.music.models.MediaMetadata?>(null) }

    var currentFormat by remember { mutableStateOf<FormatEntity?>(null) }

    val playerConnection = LocalPlayerConnection.current
    val context = LocalContext.current
    val clipboardManager = LocalClipboard.current

    LaunchedEffect(Unit, videoId) {
        // Subsonic: YouTube.getMediaInfo disabled
        // info = YouTube.getMediaInfo(videoId).getOrNull()
        info = null
    }
    LaunchedEffect(Unit, videoId) {
        database.song(videoId).collect {
            song = it
        }
    }
    LaunchedEffect(Unit, videoId) {
        // If song is not in database, get metadata from current playing track
        if (song == null && playerConnection != null) {
            val currentItem = playerConnection.player.currentMediaItem
            if (currentItem?.mediaId == videoId) {
                fallbackMetadata = currentItem.localConfiguration?.tag as? com.metrolist.music.models.MediaMetadata
            }
        }
    }
    LaunchedEffect(Unit, videoId) {
        database.format(videoId).collect {
            currentFormat = it
        }
    }

    LazyColumn(
        state = rememberLazyListState(),
        modifier = Modifier
            .padding(
                windowInsets
                    .asPaddingValues()
            )
            .padding(horizontal = 16.dp)
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (song != null || fallbackMetadata != null) {
            item(contentType = "TitleDetails") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.info),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = stringResource(R.string.details),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                }
            }

            item(contentType = "MediaDetails") {
                Column {
                    val baseList = listOf(
                        stringResource(R.string.song_title) to (song?.title ?: fallbackMetadata?.title),
                        stringResource(R.string.song_artists) to (song?.artists?.joinToString { it.name } ?: fallbackMetadata?.artists?.joinToString { it.name }),
                        stringResource(R.string.media_id) to (song?.id ?: fallbackMetadata?.id)
                    )
                    val extendedList = baseList + if (currentFormat != null) {
                        val cf = currentFormat
                        var avgBitrateBps: Int? = null
                        val songDurationSeconds = song?.song?.duration ?: -1
                        if (songDurationSeconds > 0 && cf?.contentLength != null && cf.contentLength > 0) {
                            avgBitrateBps = ((cf.contentLength * 8) / songDurationSeconds).toInt()
                        }
                        val displayedBitrate = avgBitrateBps?.takeIf { it > 0 }?.let { "${it / 1000} Kbps (avg)" }
                            ?: cf?.bitrate?.takeIf { it > 0 }?.let { "${it / 1000} Kbps" }
                        listOf(
                            "Itag" to cf?.itag?.toString(),
                            stringResource(R.string.mime_type) to cf?.mimeType,
                            stringResource(R.string.codecs) to cf?.codecs,
                            stringResource(R.string.bitrate) to displayedBitrate,
                            stringResource(R.string.sample_rate) to cf?.sampleRate?.let { "$it Hz" },
                            stringResource(R.string.loudness) to cf?.loudnessDb?.let { "$it dB" },
                            stringResource(R.string.volume) to if (playerConnection != null) "${(playerConnection.player.volume * 100).toInt()}%" else null,
                            stringResource(R.string.file_size) to cf?.contentLength?.let { Formatter.formatShortFileSize(context, it) }
                        )
                    } else emptyList()

                    extendedList.forEach { (label, text) ->
                        val displayText = text ?: stringResource(R.string.unknown)
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier =
                            Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        // LocalClipboard API may not expose direct setText; use Android ClipboardManager
                                        val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("text", displayText))
                                        Toast.makeText(
                                            context,
                                            R.string.copied,
                                            Toast.LENGTH_SHORT
                                        )
                                            .show()
                                    },
                                )
                                .padding(horizontal = 16.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        // Only show Information section if we have YouTube info (Subsonic tracks skip this)
        if (info != null) {
            item(contentType = "TitleMediaInfo") {
                Text(
                    text = stringResource(R.string.information),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (info != null) {
            if (song == null)
                item(contentType = "MediaTitle") {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Top,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Text(
                            text = "" + info?.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Start
                        )
                    }
                }

            item(contentType = "MediaAuthor") {
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Top,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.artists),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Start
                    )
                    BasicText(
                        text = "" + info?.author,
                        style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp)
                    )
                }
            }
            item(contentType = "MediaDescription") {
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Top,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.description),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Start
                    )
                    BasicText(
                        text = info?.description ?: "",
                        style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                        modifier = Modifier
                            .padding(all = 16.dp)
                    )
                }
            }
            item(contentType = "MediaNumbers") {
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Top,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.numbers),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Start
                    )
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(all = 16.dp)
                    ) {
                        Column {
                            BasicText(
                                text = stringResource(R.string.subscribers),
                                style = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.onBackground),
                            )
                            BasicText(
                                text = info?.subscribers ?: "",
                                style = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.onBackground),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        Column {
                            BasicText(
                                text = stringResource(R.string.views),
                                style = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.onBackground),
                                modifier = Modifier
                            )
                            BasicText(
                                text = "" + info?.viewCount?.toInt()
                                    ?.let { numberFormatter(it) },
                                style = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.onBackground),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        Column {
                            BasicText(
                                text = stringResource(R.string.likes),
                                style = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.onBackground),
                                modifier = Modifier
                            )
                            BasicText(
                                text = "" + info?.like?.toInt()?.let { numberFormatter(it) },
                                style = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.onBackground),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        Column {
                            BasicText(
                                text = stringResource(R.string.dislikes),
                                style = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.onBackground),
                                modifier = Modifier
                            )
                            BasicText(
                                text = "" + info?.dislike?.toInt()?.let { numberFormatter(it) },
                                style = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.onBackground),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }
        // No else shimmer needed; Subsonic tracks have no YouTube info
    }
}
