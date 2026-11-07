package com.animeav.tv.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cronet.CronetDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import com.animeav.tv.AnimeAvApp
import com.animeav.tv.data.AnimeAvClient
import com.animeav.tv.data.DownloadEntry
import com.animeav.tv.data.PlaybackSource
import com.animeav.tv.ui.components.Chip
import com.animeav.tv.ui.components.ErrorState
import com.animeav.tv.ui.components.FocusIconButton
import com.animeav.tv.ui.components.LoadingState
import com.animeav.tv.ui.components.PlayerControlButton
import com.animeav.tv.ui.components.SecondaryButton
import com.animeav.tv.ui.components.formatPlayerTime
import com.animeav.tv.ui.theme.AnimeRed
import com.animeav.tv.ui.theme.SurfaceDark
import com.animeav.tv.ui.theme.TextSecondary
import com.animeav.tv.ui.vm.PlayerViewModel
import com.animeav.tv.ui.vm.appViewModelFactory
import com.animeav.tv.work.DownloadScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.concurrent.Executors

@Composable
fun PlayerScreen(
    slug: String,
    initialNumber: Int,
    command: PlayerCommand? = null,
    commandSeq: Int = 0,
    onDialogVisibilityChanged: (Boolean) -> Unit = {},
    onBack: () -> Unit,
    onOpenWeb: (url: String, title: String) -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as AnimeAvApp
    val vm: PlayerViewModel = viewModel(
        factory = appViewModelFactory { PlayerViewModel(it.repository, it.settings) },
    )
    val state by vm.state.collectAsStateWithLifecycle()

    var currentNumber by remember { mutableIntStateOf(initialNumber) }
    var showOptions by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var interaction by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(true) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var selectedAction by remember { mutableStateOf(PlayerAction.PLAY_PAUSE) }
    var selectedRow by remember { mutableIntStateOf(0) }
    val firstOptionFocus = remember { FocusRequester() }

    fun revealControls() {
        if (!controlsVisible || selectedAction == PlayerAction.NONE) {
            selectedAction = PlayerAction.PLAY_PAUSE
            selectedRow = 0
        }
        controlsVisible = true
        interaction += 1
    }

    val trackSelector = remember {
        DefaultTrackSelector(context).apply {
            val (w, h) = when (app.settings.maxQuality) {
                "1080" -> 1920 to 1080
                "720" -> 1280 to 720
                "480" -> 854 to 480
                else -> 0 to 0
            }
            if (w > 0 && h > 0) {
                parameters = buildUponParameters().setMaxVideoSize(w, h).build()
            }
        }
    }

    val player = remember {
        ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .build()
            .apply { playWhenReady = true }
    }

    val currentNumberState = rememberUpdatedState(currentNumber)
    val mediaState = rememberUpdatedState(state.media)
    val sourceState = rememberUpdatedState(state.source)
    val embedsState = rememberUpdatedState(state.embeds)

    val cronetExecutor = remember { Executors.newSingleThreadExecutor() }
    val fallbackLaunched = remember { java.util.concurrent.atomic.AtomicBoolean(false) }

    val canDownload = (state.source as? PlaybackSource.Native)
        ?.server?.equals("MP4Upload", true) == true
    val hasNext = currentNumber < (state.media?.episodes?.size ?: 0)

    fun startDownload() {
        val media = state.media
        val native = state.source as? PlaybackSource.Native
        if (media != null && native != null) {
            val fileName = "$slug-$currentNumber.mp4"
            val entry = DownloadEntry(
                fileName = fileName,
                slug = slug,
                title = media.title,
                episodeNumber = currentNumber,
                mediaId = media.id,
            )
            app.downloadsStore.upsert(entry)
            DownloadScheduler.enqueue(context, entry, native.url)
            android.widget.Toast.makeText(
                context,
                "Descarga añadida",
                android.widget.Toast.LENGTH_SHORT,
            ).show()
        }
        showOptions = false
    }

    fun goNext() {
        state.media?.anilistId?.let { app.syncAniList(it, currentNumber) }
        currentNumber += 1
        showOptions = false
    }

    fun moveSelection(delta: Int) {
        val bottom = buildList {
            add(PlayerAction.SEEK_BACK)
            add(PlayerAction.PLAY_PAUSE)
            add(PlayerAction.SEEK_FORWARD)
            if (hasNext) add(PlayerAction.NEXT)
        }
        val top = buildList {
            add(PlayerAction.OPTIONS)
            if (canDownload) add(PlayerAction.DOWNLOAD)
            if (hasNext) add(PlayerAction.NEXT)
        }
        val options = if (selectedRow == 0) bottom else top
        val current = options.indexOf(selectedAction).coerceAtLeast(0)
        val next = (current + delta).coerceIn(0, options.lastIndex)
        selectedAction = options[next]
        interaction += 1
    }

    fun executeAction(action: PlayerAction) {
        when (action) {
            PlayerAction.SEEK_BACK ->
                player.seekTo((player.currentPosition - 10_000L).coerceAtLeast(0L))
            PlayerAction.PLAY_PAUSE ->
                if (player.isPlaying) player.pause() else player.play()
            PlayerAction.SEEK_FORWARD ->
                player.seekTo(player.currentPosition + 10_000L)
            PlayerAction.NEXT -> goNext()
            PlayerAction.OPTIONS -> showOptions = true
            PlayerAction.DOWNLOAD -> startDownload()
            PlayerAction.NONE -> Unit
        }
        interaction += 1
    }

    LaunchedEffect(slug, currentNumber) {
        vm.load(slug, currentNumber)
    }

    LaunchedEffect(showOptions) {
        onDialogVisibilityChanged(showOptions)
    }

    LaunchedEffect(commandSeq) {
        when (command) {
            PlayerCommand.OPEN_OPTIONS -> {
                showOptions = true
                revealControls()
            }
            PlayerCommand.NAV_LEFT -> {
                if (!controlsVisible) {
                    player.seekTo((player.currentPosition - 10_000L).coerceAtLeast(0L))
                    revealControls()
                } else {
                    moveSelection(-1)
                }
            }
            PlayerCommand.NAV_RIGHT -> {
                if (!controlsVisible) {
                    player.seekTo(player.currentPosition + 10_000L)
                    revealControls()
                } else {
                    moveSelection(1)
                }
            }
            PlayerCommand.NAV_UP -> {
                if (!controlsVisible) {
                    revealControls()
                } else {
                    selectedRow = 1
                    selectedAction = PlayerAction.OPTIONS
                    interaction += 1
                }
            }
            PlayerCommand.NAV_DOWN -> {
                if (!controlsVisible) {
                    revealControls()
                } else {
                    selectedRow = 0
                    selectedAction = PlayerAction.PLAY_PAUSE
                    interaction += 1
                }
            }
            PlayerCommand.CONFIRM -> {
                if (!controlsVisible) {
                    if (player.isPlaying) player.pause() else player.play()
                    revealControls()
                } else {
                    executeAction(selectedAction)
                }
            }
            null -> Unit
        }
    }

    DisposableEffect(Unit) {
        onDispose { cronetExecutor.shutdown() }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    mediaState.value?.anilistId?.let { app.syncAniList(it, currentNumberState.value) }
                    val next = currentNumberState.value + 1
                    val total = mediaState.value?.episodes?.size ?: 0
                    if (next <= total) {
                        currentNumber = next
                    } else {
                        onBack()
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val currentSource = sourceState.value
                val hlsEmbed = embedsState.value.firstOrNull {
                    it.server.equals("HLS", true) || it.url.contains("zilla-networks.com", true)
                }
                if (currentSource is PlaybackSource.Native &&
                    currentSource.server.equals("HLS", true) &&
                    hlsEmbed != null &&
                    fallbackLaunched.compareAndSet(false, true)
                ) {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        onOpenWeb(hlsEmbed.url, mediaState.value?.title.orEmpty())
                    }
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                player.playWhenReady = false
                player.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(player) {
        while (isActive) {
            position = player.currentPosition.coerceAtLeast(0L)
            duration = player.duration.coerceAtLeast(0L)
            isPlaying = player.isPlaying
            delay(500)
        }
    }

    LaunchedEffect(controlsVisible, interaction) {
        if (controlsVisible) {
            delay(6000)
            controlsVisible = false
            selectedAction = PlayerAction.NONE
        }
    }

    LaunchedEffect(state.source, currentNumber) {
        when (val source = state.source) {
            is PlaybackSource.Native -> {
                val factory = if (source.server.equals("HLS", true)) {
                    CronetDataSource.Factory(app.cronetEngine, cronetExecutor)
                        .setDefaultRequestProperties(source.headers)
                } else {
                    DefaultHttpDataSource.Factory()
                        .setDefaultRequestProperties(source.headers)
                        .setUserAgent(AnimeAvClient.USER_AGENT)
                        .setAllowCrossProtocolRedirects(true)
                }
                val mediaItem = if (source.server.equals("HLS", true)) {
                    MediaItem.Builder()
                        .setUri(source.url)
                        .setMimeType(MimeTypes.APPLICATION_M3U8)
                        .build()
                } else {
                    MediaItem.fromUri(source.url)
                }
                val mediaSource = DefaultMediaSourceFactory(factory)
                    .createMediaSource(mediaItem)
                player.setMediaSource(mediaSource)
                player.prepare()
                val progress = app.library.progressFor(slug)
                if (progress != null &&
                    progress.episodeNumber == currentNumber &&
                    progress.positionMs > 0L
                ) {
                    player.seekTo(progress.positionMs)
                }
                player.play()
            }
            is PlaybackSource.Web -> {
                val media = state.media
                if (media != null && player.duration > 0L) {
                    app.library.saveProgress(
                        slug = slug,
                        title = media.title,
                        mediaId = media.id,
                        episodeNumber = currentNumber,
                        positionMs = player.currentPosition,
                        durationMs = player.duration,
                    )
                }
                onOpenWeb(source.url, media?.title.orEmpty())
            }
            null -> Unit
        }
    }

    val handleBack: () -> Unit = {
        val media = state.media
        if (media != null) {
            if (player.duration > 0L) {
                app.library.saveProgress(
                    slug = slug,
                    title = media.title,
                    mediaId = media.id,
                    episodeNumber = currentNumber,
                    positionMs = player.currentPosition,
                    durationMs = player.duration,
                )
            }
            media.anilistId?.let { app.syncAniList(it, currentNumber) }
        }
        onBack()
    }

    BackHandler {
        when {
            showOptions -> showOptions = false
            controlsVisible -> controlsVisible = false
            else -> handleBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        when {
            state.loading -> LoadingState("Preparando reproducción…")
            state.error != null -> ErrorState(state.error ?: "Error") { vm.retry() }
            else -> {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = player
                            useController = false
                            isFocusable = false
                            isFocusableInTouchMode = false
                            descendantFocusability = android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS
                            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                            setBackgroundColor(android.graphics.Color.BLACK)
                        }
                    },
                    update = { view -> view.player = player },
                    modifier = Modifier.fillMaxSize(),
                )

                AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    PlayerOverlay(
                        title = state.media?.title.orEmpty(),
                        episodeLabel = buildString {
                            append("Episodio $currentNumber")
                            state.selectedServer?.let { append(" · $it") }
                            state.episode?.title?.let { append(" · $it") }
                        },
                        isPlaying = isPlaying,
                        position = position,
                        duration = duration,
                        hasNext = hasNext,
                        canDownload = canDownload,
                        selectedAction = selectedAction,
                        onBack = handleBack,
                        onOptions = {
                            showOptions = true
                            revealControls()
                        },
                        onDownload = { startDownload() },
                        onPlayPause = {
                            if (player.isPlaying) player.pause() else player.play()
                            revealControls()
                        },
                        onSeekBack = {
                            player.seekTo((player.currentPosition - 10_000L).coerceAtLeast(0L))
                            revealControls()
                        },
                        onSeekForward = {
                            player.seekTo(player.currentPosition + 10_000L)
                            revealControls()
                        },
                        onNext = { goNext() },
                    )
                }
            }
        }
    }

    if (showOptions) {
        LaunchedEffect(Unit) {
            runCatching { firstOptionFocus.requestFocus() }
        }
        Dialog(onDismissRequest = { showOptions = false }) {
            Column(
                modifier = Modifier
                    .width(520.dp)
                    .background(SurfaceDark, RoundedCornerShape(18.dp))
                    .padding(24.dp),
            ) {
                Text("Opciones del episodio", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Elige servidor o usa las acciones del episodio.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(16.dp))

                Text("Servidores", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(8.dp))
                state.embeds.forEachIndexed { index, embed ->
                    Chip(
                        text = embed.server.ifBlank { "Servidor ${index + 1}" },
                        selected = embed.server == state.selectedServer,
                        onClick = {
                            showOptions = false
                            vm.selectServer(embed.server)
                        },
                        modifier = if (index == 0) {
                            Modifier.focusRequester(firstOptionFocus)
                        } else {
                            Modifier
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                }

                Spacer(Modifier.height(12.dp))

                if (canDownload) {
                    SecondaryButton(
                        text = "Descargar episodio",
                        onClick = { startDownload() },
                    )
                    Spacer(Modifier.height(8.dp))
                }

                if (hasNext) {
                    SecondaryButton(
                        text = "Siguiente episodio",
                        onClick = { goNext() },
                    )
                    Spacer(Modifier.height(8.dp))
                }

                SecondaryButton(
                    text = "Cerrar",
                    onClick = { showOptions = false },
                )
            }
        }
    }
}

@Composable
private fun PlayerOverlay(
    title: String,
    episodeLabel: String,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    hasNext: Boolean,
    canDownload: Boolean,
    selectedAction: PlayerAction,
    onBack: () -> Unit,
    onOptions: () -> Unit,
    onDownload: () -> Unit,
    onPlayPause: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onNext: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.82f),
                    0.35f to Color.Transparent,
                    0.65f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.88f),
                ),
            )
            .padding(horizontal = 28.dp, vertical = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FocusIconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = episodeLabel,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(12.dp))
            Chip(
                text = "Opciones",
                selected = selectedAction == PlayerAction.OPTIONS,
                onClick = onOptions,
            )
            if (canDownload) {
                Spacer(Modifier.width(8.dp))
                Chip(
                    text = "Descargar",
                    selected = selectedAction == PlayerAction.DOWNLOAD,
                    onClick = onDownload,
                )
            }
            if (hasNext) {
                Spacer(Modifier.width(8.dp))
                Chip(
                    text = "Siguiente",
                    selected = selectedAction == PlayerAction.NEXT,
                    onClick = onNext,
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerControlButton(
                icon = Icons.Filled.Replay10,
                description = "Retroceder 10 segundos",
                onClick = onSeekBack,
                highlighted = selectedAction == PlayerAction.SEEK_BACK,
            )
            Spacer(Modifier.width(24.dp))
            PlayerControlButton(
                icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                description = if (isPlaying) "Pausar" else "Reproducir",
                onClick = onPlayPause,
                large = true,
                highlighted = selectedAction == PlayerAction.PLAY_PAUSE,
            )
            Spacer(Modifier.width(24.dp))
            PlayerControlButton(
                icon = Icons.Filled.Forward10,
                description = "Avanzar 10 segundos",
                onClick = onSeekForward,
                highlighted = selectedAction == PlayerAction.SEEK_FORWARD,
            )
            if (hasNext) {
                Spacer(Modifier.width(24.dp))
                PlayerControlButton(
                    icon = Icons.Filled.SkipNext,
                    description = "Siguiente episodio",
                    onClick = onNext,
                    highlighted = selectedAction == PlayerAction.NEXT,
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatPlayerTime(position),
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.width(64.dp),
            )
            LinearProgressIndicator(
                progress = {
                    if (duration > 0L) {
                        (position.toFloat() / duration).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                },
                color = AnimeRed,
                trackColor = Color.White.copy(alpha = 0.25f),
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
            )
            Text(
                text = formatPlayerTime(duration),
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier
                    .width(64.dp)
                    .padding(start = 12.dp),
            )
        }
    }
}
