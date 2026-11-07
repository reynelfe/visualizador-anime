# Análisis del APK de AnikenTV

Archivo: `AnikenTV.apk`
SHA-256: `8eaecec805b68c5be5d57ec6953f6a9b7499eda65bc5c81e17723395a74380d7`
Herramienta: REA (`inspect_android_package`, `decode_android_resources`, `search_android_classes`).

## Identidad

| Campo | Valor |
| --- | --- |
| Package | `com.shubh.anililitv` |
| Versión | `1.2.4` (código 47) |
| minSdk | 22 (Android 5.1) |
| targetSdk | 36 |
| Clases | 10.655 |
| Recursos | 1.534 |
| Firmas | No verificadas por el proveedor |

El repositorio público de AnikenTV solo contiene el README y las capturas; no publica el código fuente. El APK es una app nativa Kotlin/Compose ofuscada con R8.

## Permisos declarados

- `INTERNET`, `ACCESS_NETWORK_STATE`
- `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`
- `POST_NOTIFICATIONS`
- `RECEIVE_BOOT_COMPLETED`
- `REQUEST_INSTALL_PACKAGES`
- `WAKE_LOCK`
- `READ_EPG_DATA`, `WRITE_EPG_DATA` (integración con la guía de TV)
- Permiso interno de receivers dinámicos

## Componentes principales

| Componente | Tipo | Función |
| --- | --- | --- |
| `com.shubh.anililitv.MainActivity` | Activity | Entrada Leanback. |
| `ShareDiagnosticsActivity` | Activity | Compartir diagnóstico. |
| `playback.PlaybackService` | Service | `MediaSessionService` de Media3. |
| `playback.EpisodeDownloadService` | Service | Descargas offline con Media3. |
| `data.reminder.ReminderRescheduleReceiver` | Receiver | Reagenda recordatorios al arrancar. |
| `data.reminder.AiringReminderReceiver` | Receiver | Recordatorios de estrenos. |
| `data.reminder.AutomaticReleaseReceiver` | Receiver | Eventos de nuevos episodios. |
| `MediaTransferReceiver`, `ReconnectionService` | Cast | Envío a Chromecast. |
| `InitializationProvider`, `SystemForegroundService`, `SystemJobService` | AndroidX | Startup, WorkManager. |
| `Room.MultiInstanceInvalidationService` | AndroidX | Room. |

## Stack detectado

- **UI**: Jetpack Compose + `androidx.tv:tv-foundation` / `tv-material`.
- **Reproducción**: Media3 ExoPlayer, HLS, MediaSession, descargas offline, Cast.
- **Datos**: Room, DataStore, WorkManager.
- **Red**: OkHttp, **Cronet** (`CronetProviderClassName = com.google.android.gms.net.PlayServicesCronetProvider`), Firebase Installations/Remote Config.
- **Cuentas**: AniList OAuth, MyAnimeList OAuth.
- **Actualizaciones**: Supabase Storage (`version.json` y APK).
- **Metadatos**: AniList GraphQL, MAL, Jikan, AniZip, TVDB.
- **Saltos de intro**: AniSkip.
- **Proveedores de streaming**: AniKoto, Miruro, ReAnime, Anikage, AniLab, AniLight, AnimeKhor, AnimeDekho, GogoAnime, Hianime, HStream, etc.

## Qué aporta Cronet

AnikenTV declara el proveedor de Cronet de Google Play Services. Cronet es la pila de red de Chromium; su fingerprint TLS y su gestión de cookies/HTTP2 se parecen a los de Chrome. Esto es relevante para los servidores protegidos por Cloudflare: un cliente OkHttp “puro” puede recibir 403 en los segmentos HLS, mientras que Cronet puede pasar como un navegador.

En nuestra app, el HLS de `player.zilla-networks.com` se sirve a un navegador real, pero devuelve 403 a `curl` y a OkHttp. Por eso la primera versión usa WebView para ese servidor. Una mejora futura es añadir `media3-datasource-cronet` con `org.chromium.net:cronet-embedded` y usar Cronet también para el HLS.

## Diferencias con AnimeAV TV

| Aspecto | AnikenTV | AnimeAV TV |
| --- | --- | --- |
| Fuente | Agregador multi-proveedor | animeav1.com |
| Metadatos | AniList/MAL/Jikan/TVDB | API interna de AnimeAV1 |
| Cuentas | AniList / MAL | Sin cuentas |
| Descargas | Sí (Media3) | No (v1) |
| Cast | Sí | No (v1) |
| Recordatorios | Sí | No (v1) |
| Actualizaciones | Supabase | Manual (APK) |
| HLS | Media3 + Cronet / WebView | WebView (v1) |
| MP4 | Proveedores propios | MP4Upload + ExoPlayer |
