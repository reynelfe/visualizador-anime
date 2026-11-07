package com.animeav.tv

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import com.animeav.tv.ui.screens.PlayerCommand
import com.animeav.tv.ui.screens.PlayerScreen
import com.animeav.tv.ui.theme.AnimeAvTheme

class PlayerActivity : ComponentActivity() {

    private var command by mutableStateOf<PlayerCommand?>(null)
    private var commandSeq by mutableIntStateOf(0)
    private var dialogOpen by mutableStateOf(false)

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (!dialogOpen && event.action == KeyEvent.ACTION_DOWN) {
            val mapped = when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER,
                -> PlayerCommand.CONFIRM
                KeyEvent.KEYCODE_DPAD_LEFT -> PlayerCommand.NAV_LEFT
                KeyEvent.KEYCODE_DPAD_RIGHT -> PlayerCommand.NAV_RIGHT
                KeyEvent.KEYCODE_DPAD_UP -> PlayerCommand.NAV_UP
                KeyEvent.KEYCODE_DPAD_DOWN -> PlayerCommand.NAV_DOWN
                KeyEvent.KEYCODE_MENU -> PlayerCommand.OPEN_OPTIONS
                else -> null
            }
            if (mapped != null) {
                command = mapped
                commandSeq += 1
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val slug = intent.getStringExtra(EXTRA_SLUG)
        if (slug.isNullOrBlank()) {
            finish()
            return
        }
        val number = intent.getIntExtra(EXTRA_NUMBER, 1)

        setContent {
            AnimeAvTheme {
                PlayerScreen(
                    slug = slug,
                    initialNumber = number,
                    command = command,
                    commandSeq = commandSeq,
                    onDialogVisibilityChanged = { dialogOpen = it },
                    onBack = { finish() },
                    onOpenWeb = { url, title ->
                        startActivity(WebPlayerActivity.intent(this, url, title))
                        finish()
                    },
                )
            }
        }
    }

    companion object {
        private const val EXTRA_SLUG = "slug"
        private const val EXTRA_NUMBER = "number"

        fun intent(context: Context, slug: String, number: Int): Intent =
            Intent(context, PlayerActivity::class.java)
                .putExtra(EXTRA_SLUG, slug)
                .putExtra(EXTRA_NUMBER, number)
    }
}
