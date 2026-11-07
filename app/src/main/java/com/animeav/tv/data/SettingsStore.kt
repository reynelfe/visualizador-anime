package com.animeav.tv.data

import android.content.Context

class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("animeav_settings", Context.MODE_PRIVATE)

    var serverPreference: ServerPreference
        get() = runCatching {
            ServerPreference.valueOf(prefs.getString(KEY_SERVER, ServerPreference.AUTO.name) ?: "")
        }.getOrDefault(ServerPreference.AUTO)
        set(value) {
            prefs.edit().putString(KEY_SERVER, value.name).apply()
        }

    var maxQuality: String
        get() = prefs.getString(KEY_QUALITY, "auto") ?: "auto"
        set(value) {
            prefs.edit().putString(KEY_QUALITY, value).apply()
        }

    var anilistToken: String?
        get() = prefs.getString(KEY_ANILIST_TOKEN, null)
        set(value) {
            prefs.edit().putString(KEY_ANILIST_TOKEN, value).apply()
        }

    var anilistClientId: String
        get() = prefs.getString(KEY_ANILIST_CLIENT_ID, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_ANILIST_CLIENT_ID, value).apply()
        }

    var anilistUserName: String?
        get() = prefs.getString(KEY_ANILIST_USER, null)
        set(value) {
            prefs.edit().putString(KEY_ANILIST_USER, value).apply()
        }

    companion object {
        private const val KEY_SERVER = "server_preference"
        private const val KEY_QUALITY = "max_quality"
        private const val KEY_ANILIST_TOKEN = "anilist_token"
        private const val KEY_ANILIST_CLIENT_ID = "anilist_client_id"
        private const val KEY_ANILIST_USER = "anilist_user"
    }
}
