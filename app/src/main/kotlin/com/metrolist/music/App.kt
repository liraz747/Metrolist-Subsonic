package com.metrolist.music

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.datastore.preferences.core.edit
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.request.CachePolicy
import coil3.request.allowHardware
import coil3.request.crossfade
import com.metrolist.lastfm.LastFM
import com.metrolist.subsonic.Subsonic
import com.metrolist.subsonic.SubsonicCredentials
import com.metrolist.music.BuildConfig
import com.metrolist.music.constants.*
import com.metrolist.music.di.ApplicationScope
import com.metrolist.music.extensions.toEnum
import com.metrolist.music.extensions.toInetSocketAddress
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import com.metrolist.music.utils.reportException
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import java.net.Proxy
import javax.inject.Inject

@HiltAndroidApp
class App : Application(), SingletonImageLoader.Factory {

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())

        applicationScope.launch {
            initializeSettings()
            observeSettingsChanges()
        }
    }

    private suspend fun initializeSettings() {
        val settings = dataStore.data.first()

        LastFM.initialize(
            apiKey = BuildConfig.LASTFM_API_KEY.takeIf { it.isNotEmpty() } ?: "",
            secret = BuildConfig.LASTFM_SECRET.takeIf { it.isNotEmpty() } ?: ""
        )

        val subsonicServerUrl = settings[SubsonicServerUrlKey]
        val subsonicUsername = settings[SubsonicUsernameKey]
        val subsonicPassword = settings[SubsonicPasswordKey]
        val subsonicToken = settings[SubsonicTokenKey]
        val subsonicSalt = settings[SubsonicSaltKey]

        if (subsonicServerUrl != null && subsonicUsername != null) {
            try {
                Subsonic.initialize(
                    SubsonicCredentials(
                        serverUrl = subsonicServerUrl,
                        username = subsonicUsername,
                        password = subsonicPassword,
                        token = subsonicToken,
                        salt = subsonicSalt
                    )
                )

                if (settings[ProxyEnabledKey] == true) {
                    settings[ProxyUrlKey]?.let {
                        try {
                            Subsonic.proxy = Proxy(
                                settings[ProxyTypeKey].toEnum(defaultValue = Proxy.Type.HTTP),
                                it.toInetSocketAddress()
                            )
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to set Subsonic proxy")
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize Subsonic")
                reportException(e)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "updates",
                getString(R.string.update_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.update_channel_desc)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun observeSettingsChanges() {
        applicationScope.launch(Dispatchers.IO) {
            dataStore.data
                .map { it[LastFMSessionKey] }
                .distinctUntilChanged()
                .collect { session ->
                    try {
                        LastFM.sessionKey = session
                    } catch (e: Exception) {
                        Timber.e("Error while loading last.fm session key. %s", e.message)
                    }
                }
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val cacheSize = dataStore.get(MaxImageCacheSizeKey, 512)

        return ImageLoader.Builder(this).apply {
            crossfade(false)
            allowHardware(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            if (cacheSize == 0) {
                diskCachePolicy(CachePolicy.DISABLED)
            } else {
                diskCache(
                    DiskCache.Builder()
                        .directory(cacheDir.resolve("coil"))
                        .maxSizeBytes(cacheSize * 1024 * 1024L)
                        .build()
                )
            }
        }.build()
    }

    companion object {
        suspend fun forgetAccount(context: Context) {
            context.dataStore.edit { settings ->
                settings.remove(SubsonicServerUrlKey)
                settings.remove(SubsonicUsernameKey)
                settings.remove(SubsonicPasswordKey)
                settings.remove(SubsonicTokenKey)
                settings.remove(SubsonicSaltKey)
            }
        }
    }
}
