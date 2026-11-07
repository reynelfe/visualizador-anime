# Verificación en Android TV

Fecha: 2026-11-07
Dispositivo: emulador Android TV 13 (API 33), 1920×1080, AOSP TV x86.
APK: `app-debug.apk` (41 MB), package `com.animeav.tv.debug`, versión `1.2.0`, ABIs `arm64-v8a`, `armeabi-v7a` y `x86`.

## Resultados

| Prueba | Resultado | Evidencia || --- | --- | --- |
| Arranque de la app | OK | Home con hero de One Piece y filas de episodios. |
| Carga de datos de AnimeAV1 | OK | `/__data.json` y decoder devalue. |
| Navegación D-pad | OK | Foco en barra, hero, chips, tarjetas y botones. |
| Catálogo | OK | 20 títulos, filtros y años 2026–2018. |
| Búsqueda | OK | `naruto` → 6 resultados. |
| Ficha de anime | OK | One Piece: 8.7, 1180 episodios, relaciones. |
| Ficha enriquecida con AniList | OK | Carátula y banner de AniList, `anilistId` disponible. |
| Reproductor nativo | OK | MP4Upload reproduce One Piece EP 1 con subtítulos. |
| Controles personalizados | OK | Pausa, -10 s, +10 s, siguiente y barra de progreso. |
| Play/pausa con CENTER | OK | `playWhenReady true → false → true`; un CENTER pausa y el siguiente reanuda. |
| Seek con D-pad | OK | DERECHA 0:52, IZQUIERDA 0:42. |
| Menú de opciones | OK | MENÚ abre servidores, descargar, siguiente y cerrar. |
| HLS por defecto | OK | Al abrir One Piece EP1 arranca en HLS (0:11 / 25:00). |
| Botones seleccionables | OK | Play, -10, Opciones y Siguiente se resaltan y navegan con el D-pad. |
| Servidor MP4Upload | OK | Reproduce con ExoPlayer. |
| Servidor Voe | Parcial | Carga anuncio y audio; hay que cerrar el anuncio. |
| Servidor Byse | Parcial | Carga la SPA y audio; vídeo no verificable por captura. |
| Servidor UPNShare | No disponible | `turboembed.com` devuelve una página vacía. |
| MP4Upload | OK | Carga con `DefaultHttpDataSource` y el tiempo avanza (0:05 → 0:12). |
| Pausa al pulsar HOME | OK | `ON_STOP: pausando reproductor`; el audio se detiene al salir al launcher. |
| Ocultación de controles | OK | El menú y los controles desaparecen a los 5 s; una tecla los muestra. |
| Cambio de servidor | OK | MENÚ abre el diálogo; se seleccionó HLS. |
| HLS nativo con Cronet | OK | ExoPlayer reproduce el HLS; tiempo 0:12 / 25:00. |
| Fallback de HLS | OK | Si el HLS nativo falla, se abre `WebPlayerActivity`. |
| Reproductor web | OK | Disponible como fallback para Voe/Byse/UPNShare. |
| Descargas offline | OK | `one-piece-1.mp4.part` crece; `downloads.json` muestra progreso. |
| Pantalla Descargas | OK | Progreso, estado y botón Cancelar. |
| Conexión AniList (UI) | OK | Campo de Client ID y botón Conectar en Ajustes. |
| Sincronización AniList | Implementada | Requiere Client ID propio para probar el OAuth completo. |
| Historial | OK | El detalle muestra “Continuar EP 1”. |
| Mi lista | OK | One Piece guardado y visible en “Mi lista”. |
| Ajustes | OK | Servidor, calidad y AniList. |
| Tests unitarios | OK | `DevalueTest` (3/3). |

## Verificación en Google TV real

Dispositivo: **4K Google TV Stick**, Android 14, CPU `armeabi-v7a` (32 bits ARM).

| Prueba | Resultado |
| --- | --- |
| Conexión ADB por red (`192.168.0.12:5555`) | OK |
| Instalación del APK universal | OK |
| Arranque de la app | OK, sin crashes |
| Carga del inicio y carátulas | OK |
| Apertura de ficha y episodio | OK |
| Reproducción con MP4Upload | OK (audio reproducido; la capa de video no se captura con `screencap`) |
| Botón Descargar visible | OK |

## Limitaciones observadas en el emulador

- `adb screencap` no captura la superficie de video del `PlayerView` ni del WebView de Chromium. En el televisor la imagen se ve; el audio y el reproductor están activos.
- La sync de AniList necesita un Client ID creado por el usuario en AniList Developer.
- La descarga de One Piece EP 1 es de 196 MB; en el emulador se verificó el progreso, no la finalización completa.
- El emulador usa WebView 101 (Android System WebView). En un Google TV actualizado será más reciente.

## Cómo reproducir la verificación

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$ANDROID_HOME/platform-tools:$PATH"

# Emulador
ANDROID_AVD_HOME="$HOME/.config/.android/avd" \
  "$ANDROID_HOME/emulator/emulator" -avd AnimeAVTV \
  -no-window -no-audio -no-snapshot -no-boot-anim \
  -gpu swiftshader_indirect -accel on &

# Instalar y abrir
adb install -r dist/AnimeAVTV-1.0.0-debug.apk
adb shell am start -n com.animeav.tv.debug/com.animeav.tv.MainActivity
```

## Capturas

Las capturas de la sesión de verificación están en `docs/capturas/`:

- `home.png` — inicio.
- `catalogo.png` — catálogo.
- `detalle.png` — ficha de One Piece.
- `reproductor-nativo.png` — reproductor nativo.
- `servidores.png` — selector de servidores.
- `busqueda.png` — búsqueda de Naruto.
- `mi-lista.png` — Mi lista.
- `ajustes.png` — ajustes.
- `descargas.png` — descargas en progreso.
- `anilist.png` — conexión con AniList.
- `hls-fallback.png` — reproductor web tras el 403 de Cronet.

## Rendimiento (1.2.6)

Medición en el **4K Google TV Stick** (Android 14, `armeabi-v7a`, 1920×1080 a 60 Hz)
con `adb shell dumpsys gfxinfo com.animeav.tv.debug` y una secuencia fija de
pulsaciones del D-pad.

| Escenario | Antes (1.2.5) | Después (1.2.6) |
| --- | --- | --- |
| Arranque (`am start -W`) | 7 503 ms | 1 251-1 352 ms |
| Secuencia de 8 pulsaciones | 72 fotogramas, 60,6 % janky, p50 46 ms | 14 fotogramas, p50 57 ms |
| Una pulsación en una fila | 16 fotogramas (~300 ms de animación) | 1-2 fotogramas |
| Scroll vertical del inicio | 12 fotogramas, p50 109 ms | 7 fotogramas, p50 61 ms |
| GPU por fotograma (filas) | 12-16 ms | 5-8 ms |
| Tamaño del APK | 41 MB (debug) | 21 MB (release, R8) |

Causas corregidas:

- `BringIntoViewSpec` por defecto de Android: recolocaba el elemento enfocado al
  30 % del viewport con `tween(125 ms)` en **cada** pulsación, lo que redibujaba
  la pantalla durante ~16 fotogramas. Se sustituye por un desplazamiento mínimo,
  con margen del 12 % y `snap()`.
- Transiciones de navegación a pantalla completa y animación de color de las
  pestañas: eliminadas.
- Capas `graphicsLayer` por tarjeta: añadían ~9 ms de GPU por fotograma; eliminadas.
- Parseo de JSON/devalue y `Jsoup` de AniList en el hilo principal: movidos a
  `Dispatchers.IO` / `Dispatchers.Default`.
- Filas del inicio compuestas de antemano (son cuatro secciones) para evitar el
  tirón de componer una fila nueva al mover el foco entre secciones.

También se añadió el APK de release con **R8** y **Baseline Profile** generado con
Macrobenchmark en el propio televisor (`app/src/release/generated/baselineProfiles/`).
