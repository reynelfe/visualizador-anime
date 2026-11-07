package com.animeav.tv

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class AniListAuthActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val clientId = intent.getStringExtra(EXTRA_CLIENT_ID)?.trim()
        if (clientId.isNullOrBlank()) {
            finish()
            return
        }

        val app = application as AnimeAvApp
        val webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.userAgentString = "Mozilla/5.0 (Linux; Android 12; SHIELD Android TV) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    return handleUrl(url)
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    handleUrl(url)
                }
            }
            loadUrl(
                "https://anilist.co/api/v2/oauth/authorize" +
                    "?client_id=${clientId.trim()}&response_type=token",
            )
        }

        setContentView(webView)
    }

    private fun handleUrl(url: String?): Boolean {
        if (url == null || !url.contains("access_token=")) return false

        val token = url.substringAfter("access_token=", "")
            .substringBefore("&")
            .trim()
        if (token.isBlank()) return true

        val app = application as AnimeAvApp
        app.settings.anilistToken = token
        lifecycleScope.launch {
            val viewer = runCatching { app.anilistClient.viewer(token) }.getOrNull()
            app.settings.anilistUserName = viewer?.name
            setResult(RESULT_OK)
            finish()
        }
        return true
    }

    companion object {
        private const val EXTRA_CLIENT_ID = "client_id"

        fun intent(context: Context, clientId: String): Intent =
            Intent(context, AniListAuthActivity::class.java)
                .putExtra(EXTRA_CLIENT_ID, clientId)
    }
}
