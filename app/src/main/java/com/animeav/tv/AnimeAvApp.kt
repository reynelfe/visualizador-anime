package com.animeav.tv

import android.app.Application
import android.graphics.Bitmap
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.animeav.tv.data.AniListClient
import com.animeav.tv.data.AnimeAvClient
import com.animeav.tv.data.AnimeRepository
import com.animeav.tv.data.DownloadsStore
import com.animeav.tv.data.LibraryStore
import com.animeav.tv.data.PlaybackResolver
import com.animeav.tv.data.SettingsStore
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.Cache
import okhttp3.OkHttpClient
import org.chromium.net.CronetEngine
import java.io.File
import java.util.concurrent.TimeUnit

class AnimeAvApp : Application(), ImageLoaderFactory {

    lateinit var json: Json
        private set
    lateinit var http: OkHttpClient
        private set
    lateinit var repository: AnimeRepository
        private set
    lateinit var library: LibraryStore
        private set
    lateinit var settings: SettingsStore
        private set

    val cronetEngine: CronetEngine by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        CronetEngine.Builder(this)
            .enableHttp2(true)
            .enableQuic(true)
            .build()
    }
    lateinit var anilistClient: AniListClient
        private set
    lateinit var downloadsStore: DownloadsStore
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun syncAniList(mediaId: Int, progress: Int) {
        val token = settings.anilistToken?.takeIf { it.isNotBlank() } ?: return
        appScope.launch {
            runCatching { anilistClient.saveProgress(token, mediaId, progress) }
        }
    }

    override fun onCreate() {
        super.onCreate()

        json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
            encodeDefaults = true
            coerceInputValues = true
        }

        http = OkHttpClient.Builder()
            .cache(Cache(File(cacheDir, "http"), 64L * 1024 * 1024))
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .addInterceptor { chain ->
                val request = chain.request()
                val host = request.url.host
                val prepared = if (host == "animeav1.com" || host.endsWith(".animeav1.com")) {
                    request.newBuilder()
                        .header("User-Agent", AnimeAvClient.USER_AGENT)
                        .header("Referer", "${AnimeAvClient.BASE_URL}/")
                        .build()
                } else {
                    request
                }
                chain.proceed(prepared)
            }
            .build()

        val aniList = AniListClient(http, json)
        anilistClient = aniList
        repository = AnimeRepository(
            client = AnimeAvClient(http, json),
            resolver = PlaybackResolver(http),
            anilist = aniList,
        )
        library = LibraryStore(this, json)
        settings = SettingsStore(this)
        downloadsStore = DownloadsStore(this, json)
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .okHttpClient { http }
            .bitmapConfig(Bitmap.Config.RGB_565)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(256L * 1024 * 1024)
                    .build()
            }
            .crossfade(false)
            .respectCacheHeaders(false)
            .build()
}
