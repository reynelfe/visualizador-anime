# Changelog

## 1.2.6 — 2026-11-07

- **Rendimiento en TV**: APK de release con R8 y Baseline Profile. El arranque en el 4K Google TV Stick baja de 7,5 s a ~1,3 s y el jank de la primera medición de 93,5 % a valores de un solo dígito por pulsación.
- **Navegación instantánea**: se sustituye el `BringIntoViewSpec` de Android (pivote al 30 % con animación de 125 ms en cada pulsación) por un desplazamiento mínimo, con margen del 12 %, y sin animación. Una pulsación del D-pad pasa de ~16-20 fotogramas a **1-2 fotogramas**.
- Se eliminan las transiciones de navegación a pantalla completa y la animación de color de las pestañas superiores.
- Se quitan las capas `graphicsLayer` por tarjeta: el tiempo de GPU por fotograma baja de ~16 ms a **6-8 ms**.
- El inicio compone sus cuatro secciones de antemano para evitar el tirón al entrar en una fila nueva.
- **Filas sin cortes**: las filas de inicio y Mi lista calculan cuántas tarjetas caben y las reparten para que nunca queden tarjetas a medias; los chips del catálogo se difuminan en el borde derecho.
- El parseo de JSON/devalue y el enriquecido con AniList se ejecutan fuera del hilo principal.
- APK de release de 21 MB (antes 41 MB en depuración).

## 1.2.5 — 2026-11-07

- **Botones del reproductor seleccionables**: el D-pad mueve la selección entre -10 s, play, +10 s y siguiente (fila inferior) y Opciones, Descargar y Siguiente (fila superior).
- CENTER activa el botón seleccionado; los controles muestran el botón activo en rojo.
- **Servidores externos en WebView directo**: UPNShare, Voe y Byse cargan su página con foco y D-pad, en lugar de un iframe.
- Se permiten los certificados inválidos de los hosts de vídeo para que carguen sus players.
- Verificado: HLS y MP4Upload funcionan; Voe reproduce tras cerrar el anuncio; Byse carga y reproduce audio; UPNShare no está disponible (turboembed vacío).
- APK 1.2.5 (41 MB, `arm64-v8a`, `armeabi-v7a`, `x86`).

## 1.2.4 — 2026-11-07

- **HLS por defecto**: `ServerPreference.AUTO` ahora elige primero el servidor HLS (ExoPlayer + Cronet) y deja MP4Upload como alternativa.
- Texto de Ajustes actualizado para reflejar el orden.
- Verificado en el 4K Google TV Stick: One Piece EP1 arranca en HLS (0:11 / 25:00).
- APK 1.2.4 (41 MB, `arm64-v8a`, `armeabi-v7a`, `x86`).

## 1.2.3 — 2026-11-07

- **Manejo de teclas movido a `PlayerActivity.dispatchKeyEvent`**: ya no depende del foco de Compose.
- **CENTER**: play/pausa; **IZQUIERDA/DERECHA**: -10 s/+10 s.
- **MENÚ**: abre el menú **Opciones del episodio** con servidores, descargar y siguiente.
- El diálogo se navega con el D-pad (el foco empieza en el primer servidor).
- Verificado en el 4K Google TV Stick: seek 0:52 → 0:42, menú de opciones y play/pausa.
- APK 1.2.3 (41 MB, `arm64-v8a`, `armeabi-v7a`, `x86`).

## 1.2.2 — 2026-11-07

- **Corregido el doble toggle de play/pausa**: el contenedor consume KeyDown y KeyUp, así que un CENTER pausa y el siguiente reanuda (verificado: `true → false → true`).
- **MP4Upload vuelve a cargar rápido**: los MP4 usan `DefaultHttpDataSource`; el HLS sigue con Cronet.
- El `PlayerView` ya no roba el foco al contenedor de controles.
- Verificado en el 4K Google TV Stick: MP4 avanza (0:05 → 0:12) y el toggle de pausa funciona.

## 1.2.1 — 2026-11-07

- **La reproducción se pausa al pulsar HOME** o al pasar la app a segundo plano. Se aplica al reproductor nativo (ExoPlayer), al reproductor web y al reproductor de descargas locales.
- Log de `ON_STOP` para verificar la pausa.
- Verificado en el 4K Google TV Stick: `ON_STOP: pausando reproductor`.

## 1.2.0 — 2026-11-07

- **Reproductor rediseñado**: controles personalizados con pausa, retroceso 10 s, avance 10 s, siguiente episodio, barra de progreso y tiempos.
- El menú superior y los controles se **ocultan a los 5 segundos**; cualquier tecla del D-pad los muestra.
- **HLS nativo con ExoPlayer + Cronet**: se añaden headers de navegador y Cloudflare deja de devolver 403. El HLS ya se reproduce en el reproductor nativo.
- Fallback automático al WebView si el HLS nativo falla.
- Rendimiento: caché de imágenes en memoria (25 %) y disco (256 MB), decodificación al tamaño real de cada tarjeta, home cargado en paralelo y menos escrituras durante las descargas.
- Verificado en un **4K Google TV Stick** (Android 14, `armeabi-v7a`): HLS 0:12 / 25:00 con controles.
- APK 1.2.0 de 41 MB con `arm64-v8a`, `armeabi-v7a` y `x86`.

## 1.1.0 — 2026-11-07

- **Cronet embedded** para reproducción nativa. El HLS de zilla sigue recibiendo 403 de Cloudflare, así que se añade fallback automático al WebView.
- **Metadatos enriquecidos desde AniList**: carátula `extraLarge`, banner, géneros, puntuación y `anilistId`.
- **Descargas offline** de MP4Upload con WorkManager: progreso, notificación, cancelación y reproductor local.
- Nueva pantalla **Descargas** en la barra superior.
- **Conexión con AniList** mediante OAuth implícito en WebView y sincronización de progreso con `SaveMediaListEntry`.
- Nuevo reproductor local para archivos descargados.
- APK universal de 40 MB con `arm64-v8a`, `armeabi-v7a` y `x86`.
- Verificado en un **4K Google TV Stick** (Android 14, `armeabi-v7a`): instalación, arranque y reproducción con MP4Upload.
- Tests del decodificador de devalue: 3/3.

## 1.0.0 — 2026-11-07

- Primera versión funcional para Android TV / Google TV.
- Inicio con destacados, últimos episodios, recién agregados y populares.
- Catálogo con filtros y paginación.
- Buscador.
- Ficha de anime con relaciones y lista de episodios.
- Reproductor nativo ExoPlayer para MP4Upload.
- Reproductor web (WebView) para HLS de AnimeAV1 y servidores alternativos.
- Selector de servidor.
- Historial, “Continuar viendo” y “Mi lista”.
- Ajustes de servidor y calidad.
