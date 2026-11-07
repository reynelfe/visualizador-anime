package com.animeav.tv.ui.screens

import android.annotation.SuppressLint
import android.net.http.SslError
import android.util.Log
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.animeav.tv.ui.components.FocusIconButton
import com.animeav.tv.ui.components.PlayerControlButton
import com.animeav.tv.ui.components.formatPlayerTime
import com.animeav.tv.ui.theme.AnimeRed
import com.animeav.tv.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebPlayerScreen(
    url: String,
    title: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val isZilla = url.contains("player.zilla-networks.com/play/")
    val m3u8Url = remember(url) {
        if (!isZilla) null
        else "https://player.zilla-networks.com/m3u8/" + url.substringAfterLast("/")
    }

    val hlsJs = remember {
        runCatching {
            context.assets.open("hls.min.js").bufferedReader().use { it.readText() }
        }.getOrDefault("")
    }

    val html = remember(url, isZilla, m3u8Url) {
        if (isZilla && m3u8Url != null) {
            buildHlsHtml(embedUrl = url, m3u8Url = m3u8Url, hlsJs = hlsJs)
        } else {
            ""
        }
    }

    val playerFocus = remember { FocusRequester() }
    val webViewRef = remember { mutableStateOf<WebView?>(null) }

    var controlsVisible by remember { mutableStateOf(true) }
    var interaction by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }

    fun revealControls() {
        controlsVisible = true
        interaction += 1
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef.value?.stopLoading()
            webViewRef.value?.destroy()
            webViewRef.value = null
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    webViewRef.value?.evaluateJavascript(PAUSE_JS, null)
                    webViewRef.value?.onPause()
                }
                Lifecycle.Event.ON_START -> {
                    webViewRef.value?.onResume()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(webViewRef.value) {
        while (isActive) {
            val webView = webViewRef.value
            if (webView != null && isZilla) {
                webView.evaluateJavascript(VIDEO_STATE_JS) { result ->
                    runCatching {
                        val json = Json.parseToJsonElement(result).jsonObject
                        isPlaying = !json["p"]!!.jsonPrimitive.boolean
                        position = json["c"]!!.jsonPrimitive.long
                        duration = json["d"]!!.jsonPrimitive.long
                    }
                }
            }
            delay(500)
        }
    }

    if (isZilla) {
        LaunchedEffect(controlsVisible, interaction) {
            if (controlsVisible) {
                delay(5000)
                controlsVisible = false
                runCatching { playerFocus.requestFocus() }
            }
        }
    } else {
        LaunchedEffect(Unit) {
            delay(5000)
            controlsVisible = false
        }
    }

    BackHandler {
        if (isZilla && controlsVisible) {
            controlsVisible = false
            runCatching { playerFocus.requestFocus() }
        } else {
            onBack()
        }
    }

    val boxModifier = if (isZilla) {
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(playerFocus)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                val webView = webViewRef.value ?: return@onKeyEvent false
                when (event.key) {
                    Key.DirectionCenter, Key.Enter -> {
                        webView.evaluateJavascript(TOGGLE_JS, null)
                        revealControls()
                        true
                    }
                    Key.DirectionLeft -> {
                        webView.evaluateJavascript(SEEK_BACK_JS, null)
                        revealControls()
                        true
                    }
                    Key.DirectionRight -> {
                        webView.evaluateJavascript(SEEK_FORWARD_JS, null)
                        revealControls()
                        true
                    }
                    Key.DirectionUp, Key.DirectionDown -> {
                        revealControls()
                        true
                    }
                    else -> false
                }
            }
    } else {
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    }

    Box(modifier = boxModifier) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    setBackgroundColor(android.graphics.Color.BLACK)
                    isFocusable = true
                    isFocusableInTouchMode = true
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        javaScriptCanOpenWindowsAutomatically = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        setSupportZoom(false)
                        builtInZoomControls = false
                        allowFileAccess = false
                        allowContentAccess = false
                        cacheMode = WebSettings.LOAD_DEFAULT
                        userAgentString = "Mozilla/5.0 (Linux; Android 12; SHIELD Android TV) " +
                            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, loadedUrl: String?) {
                            Log.d("WebPlayer", "FINISHED $loadedUrl")
                        }

                        override fun onReceivedSslError(
                            view: WebView?,
                            handler: SslErrorHandler?,
                            error: SslError?,
                        ) {
                            Log.d(
                                "WebPlayer",
                                "SSL error ${error?.primaryError} at ${error?.url}",
                            )
                            handler?.proceed()
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                            Log.d("WebPlayer", "${message.message()} @ ${message.lineNumber()}")
                            return true
                        }

                        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                            Log.d("WebPlayer", "CUSTOM VIEW")
                        }
                    }

                    if (isZilla) {
                        loadDataWithBaseURL(
                            "https://player.zilla-networks.com/",
                            html,
                            "text/html",
                            "utf-8",
                            null,
                        )
                    } else {
                        loadUrl(url)
                        requestFocus()
                    }
                    webViewRef.value = this
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            if (isZilla) {
                ZillaControls(
                    title = title,
                    isPlaying = isPlaying,
                    position = position,
                    duration = duration,
                    onBack = onBack,
                    onPlayPause = {
                        webViewRef.value?.evaluateJavascript(TOGGLE_JS, null)
                        revealControls()
                    },
                    onSeekBack = {
                        webViewRef.value?.evaluateJavascript(SEEK_BACK_JS, null)
                        revealControls()
                    },
                    onSeekForward = {
                        webViewRef.value?.evaluateJavascript(SEEK_FORWARD_JS, null)
                        revealControls()
                    },
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent),
                            ),
                        )
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title.ifBlank { "Reproductor web" },
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = "Usa el D-pad y CENTER para controlar el reproductor del servidor",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun ZillaControls(
    title: String,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
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
                    text = title.ifBlank { "Reproductor web" },
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "HLS · reproductor web",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
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
            )
            Spacer(Modifier.width(24.dp))
            PlayerControlButton(
                icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                description = if (isPlaying) "Pausar" else "Reproducir",
                onClick = onPlayPause,
                large = true,
            )
            Spacer(Modifier.width(24.dp))
            PlayerControlButton(
                icon = Icons.Filled.Forward10,
                description = "Avanzar 10 segundos",
                onClick = onSeekForward,
            )
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

private fun buildHlsHtml(embedUrl: String, m3u8Url: String, hlsJs: String): String {
    val safeEmbed = embedUrl.replace("\"", "%22")
    val safeM3u8 = m3u8Url.replace("\"", "%22")
    val safeJs = hlsJs.replace("</script>", "<\\/script>")
    return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
          <style>
            html, body { margin:0; padding:0; width:100%; height:100%; background:#000; overflow:hidden; }
            #video { width:100%; height:100%; object-fit:contain; background:#000; }
          </style>
        </head>
        <body>
          <video id="video" autoplay playsinline></video>
          <script>$safeJs</script>
          <script>
            (function() {
              var video = document.getElementById('video');
              var src = "$safeM3u8";
              var embed = "$safeEmbed";
              var started = false;

              function fallback() {
                var frame = document.createElement('iframe');
                frame.src = embed;
                frame.style.cssText = 'width:100%;height:100%;border:0';
                frame.setAttribute('allowfullscreen', '');
                document.body.innerHTML = '';
                document.body.appendChild(frame);
              }

              function startHls() {
                if (started) return;
                started = true;
                if (window.Hls && Hls.isSupported()) {
                  var hls = new Hls({ enableWorker: true });
                  hls.loadSource(src);
                  hls.attachMedia(video);
                  hls.on(Hls.Events.MANIFEST_PARSED, function() {
                    video.play().catch(function() {});
                  });
                  hls.on(Hls.Events.ERROR, function(event, data) {
                    if (data && data.fatal) fallback();
                  });
                } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
                  video.src = src;
                  video.play().catch(function() {});
                } else {
                  fallback();
                }
              }
              setTimeout(startHls, 1000);
            })();
          </script>
        </body>
        </html>
    """.trimIndent()
}

private const val VIDEO_STATE_JS = """
(function() {
  var v = document.getElementById('video');
  if (!v) return {p:true,c:0,d:0};
  return {p:v.paused,c:Math.round((v.currentTime||0)*1000),d:Math.round((v.duration||0)*1000)};
})()
"""

private const val TOGGLE_JS = """
(function() {
  var v = document.getElementById('video');
  if (!v) return;
  if (v.paused) v.play(); else v.pause();
})()
"""

private const val PAUSE_JS = """
(function() {
  var v = document.getElementById('video');
  if (v) v.pause();
})()
"""

private const val SEEK_BACK_JS = """
(function() {
  var v = document.getElementById('video');
  if (!v) return;
  v.currentTime = Math.max(0, v.currentTime - 10);
})()
"""

private const val SEEK_FORWARD_JS = """
(function() {
  var v = document.getElementById('video');
  if (!v) return;
  var max = isFinite(v.duration) ? v.duration : v.currentTime + 10;
  v.currentTime = Math.min(max, v.currentTime + 10);
})()
"""
