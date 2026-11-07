package com.animeav.tv

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.animeav.tv.ui.screens.LocalPlayerScreen
import com.animeav.tv.ui.theme.AnimeAvTheme

class LocalPlayerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val path = intent.getStringExtra(EXTRA_PATH)
        if (path.isNullOrBlank()) {
            finish()
            return
        }
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()

        setContent {
            AnimeAvTheme {
                LocalPlayerScreen(
                    path = path,
                    title = title,
                    onBack = { finish() },
                )
            }
        }
    }

    companion object {
        private const val EXTRA_PATH = "path"
        private const val EXTRA_TITLE = "title"

        fun intent(context: Context, path: String, title: String): Intent =
            Intent(context, LocalPlayerActivity::class.java)
                .putExtra(EXTRA_PATH, path)
                .putExtra(EXTRA_TITLE, title)
    }
}
