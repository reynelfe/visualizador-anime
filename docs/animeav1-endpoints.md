# AnimeAV1: endpoints y formato de datos

Documento de ingeniería inversa del sitio `https://animeav1.com` utilizado para construir el cliente Android TV.

Fecha del análisis: 2026-11-07.

## 1. Plataforma

- SPA construida con **SvelteKit** (Svelte 5).
- Los datos de cada ruta se sirven en el fichero hermano `__data.json`:
  - `/` → `/__data.json`
  - `/catalogo` → `/catalogo/__data.json`
  - `/media/{slug}` → `/media/{slug}/__data.json`
  - `/media/{slug}/{numero}` → `/media/{slug}/{numero}/__data.json`
- El HTML inicial también incluye el payload, pero `__data.json` es más limpio y estable para un cliente.
- Las peticiones necesitan un `User-Agent` de navegador. No se ha observado un token ni una firma obligatoria.

## 2. Formato devalue de SvelteKit

`__data.json` tiene esta forma:

```json
{
  "type": "data",
  "nodes": [
    null,
    { "type": "data", "data": [ ... ], "uses": { ... } }
  ]
}
```

El array `data` es un **array plano**. Los enteros dentro de objetos y arrays son **índices** a posiciones del mismo array. Ejemplo simplificado:

```json
[
  { "media": 1, "episode": 4 },
  { "title": 2, "score": 3, "slug": 5 },
  "One Piece",
  8.73,
  { "id": 6, "number": 1 },
  "one-piece",
  3433
]
```

El objeto raíz está en el índice `0`. `media` apunta al índice `1`; `title` apunta al índice `2`; `score` al `3`; etc. Los valores literales (strings, números, booleanos, `null`) se devuelven tal cual cuando se resuelve su índice.

Valores especiales:

- `{"type":"Date","value":"..."}` → fecha.
- `{"type":"BigInt","value":"..."}` → entero grande.
- `{"type":"undefined"}` → `null`.
- `-1` / `-2` → `undefined` / hueco (se tratan como `null`).

El decodificador del proyecto está en `app/src/main/java/com/animeav/tv/data/Devalue.kt`.

## 3. Inicio (`/__data.json`)

Objeto raíz con:

- `featured`: lista de `MediaDto`.
- `latestEpisodes`: lista de episodios recientes (`id`, `number`, `media`, `createdAt`, `publishedAt`, `commentsCount`).
- `latestMedia`: lista de `MediaDto` recién agregados.

## 4. Catálogo (`/catalogo/__data.json`)

Parámetros observados:

| Parámetro | Ejemplo | Notas |
| --- | --- | --- |
| `page` | `2` | Paginación de 20 elementos. |
| `genre` | `accion`, `shounen` | Slug del género. |
| `category` | `tv-anime`, `pelicula`, `ova`, `especial`, `ona` | Slug de la categoría. |
| `year` | `2024` | Año de estreno. |
| `status` | `1` | 1 = en emisión, 2 = finalizado, 3 = próximamente. |
| `order` | `recent`, `popular`, `score` | Orden del listado. |

Respuesta:

```json
{
  "results": [ ... ],
  "total": 1000,
  "categoriesIdsMap": { "1": { "id": 1, "name": "TV Anime", "slug": "tv-anime" } },
  "genresIdsMap": { "1": { "id": 1, "name": "Acción", "slug": "accion" } },
  "years": [1990, 2026],
  "orderKey": "recent",
  "pagination": {
    "currentPage": 1,
    "recordsPerPage": 20,
    "totalPages": 50,
    "totalRecords": 1000
  }
}
```

`total` está limitado a 1000 en la respuesta observada; `pagination.totalPages` es la fuente fiable para paginar.

## 5. Búsqueda

```
POST https://animeav1.com/api/search
Content-Type: application/json

{"query":"one piece"}
```

Respuesta:

```json
[
  { "id": "197", "title": "One Piece", "slug": "one-piece" },
  { "id": "4415", "title": "One Piece: Heroines", "slug": "one-piece-heroines" }
]
```

No requiere token. Un POST de formulario (`application/x-www-form-urlencoded`) devuelve 403 por CSRF; hay que enviar JSON.

## 6. Ficha (`/media/{slug}/__data.json`)

Objeto `media` con:

- `id`, `slug`, `title`, `synopsis`, `score`, `status`, `startDate`, `endDate`.
- `genres`: lista de `{id, name, slug}`.
- `category`: `{id, name, slug}`.
- `episodes`: lista de `{id, number}` (sin título en la ficha).
- `relations`: lista de `{type, destination: {id, slug, title, startDate}}`.
- `poster` y `backdrop`: normalmente `null`; la CDN construye las imágenes por id.

## 7. Episodio (`/media/{slug}/{numero}/__data.json`)

Objeto raíz con:

- `media`: ficha del anime.
- `episode`: `{id, mediaId, title, number, season, relativeNumber, variants, filler}`.
- `embeds`: mapa por variante (`SUB`, `DUB`) con la lista de servidores.
- `downloads`: mapa por variante con enlaces de descarga.

Ejemplo real (One Piece, episodio 1):

```json
{
  "embeds": {
    "SUB": [
      { "server": "HLS",       "url": "https://player.zilla-networks.com/play/7601658844858539e438dceee32e7924" },
      { "server": "UPNShare",  "url": "https://animeav1.uns.bio/#3zq51b" },
      { "server": "Voe",       "url": "https://voe.sx/e/bkvxsyie9vmw" },
      { "server": "Byse",      "url": "https://byselapuix.com/e/48hpdh861199" },
      { "server": "MP4Upload", "url": "https://www.mp4upload.com/embed-iwh1k72esxo1.html" }
    ]
  }
}
```

## 8. Imágenes

- Portada: `https://cdn.animeav1.com/covers/{mediaId}.jpg`
- Fondo: `https://cdn.animeav1.com/backdrops/{mediaId}.jpg`
- Miniatura de episodio: `https://cdn.animeav1.com/thumbnails/{mediaId}.jpg`

Los campos `poster`/`backdrop` del JSON suelen ser `null`; la CDN responde correctamente por id.

## 9. Servidores de vídeo

### HLS (zilla)

- URL del embed: `https://player.zilla-networks.com/play/{32 hex}`
- Playlist: `https://player.zilla-networks.com/m3u8/{32 hex}`
- El playlist es válido (`#EXTM3U`, `#EXT-X-MAP` y segmentos `.html`).
- **Cloudflare** permite el playlist y los segmentos a un navegador real, pero devuelve 403 a `curl`, a OkHttp y también a **Cronet 141** (probado con `media3-datasource-cronet`). El bloqueo no se resuelve solo con el fingerprint TLS: parece requerir cookies de challenge (`cf_clearance`) que el WebView obtiene al cargar el player.
- Por eso el cliente intenta HLS nativo con Cronet y, al recibir el 403, abre automáticamente el WebView con el embed.
- El player de zilla exige estar dentro de un iframe (`window.self !== window.top`). La app carga una página local con un iframe que ocupa toda la pantalla.

### MP4Upload

- Embed: `https://www.mp4upload.com/embed-{id}.html`
- El HTML contiene una URL directa del tipo:
  `https://a4.mp4upload.com:183/d/{token}/video.mp4`
- Responde `206 Partial Content` y `video/mp4`; ExoPlayer la reproduce con los headers `Referer: https://www.mp4upload.com/` y un `User-Agent` de navegador.
- Es la fuente nativa preferida.

### Voe

- `https://voe.sx/e/{id}` redirige a `https://shannoncoldalready.com/e/{id}`.
- La página requiere un token de sesión (`/api2/session/generate-token`) y un popup; no es viable para ExoPlayer. Se abre en WebView.

### Byse

- `https://byselapuix.com/e/{id}` es una SPA (Vite). Se abre en WebView.

### UPNShare

- `https://animeav1.uns.bio/#{id}` es una página con un botón de reproducción. Se abre en WebView.

## 10. Notas de mantenimiento

- Si AnimeAV1 cambia el formato de `__data.json`, actualizar `Devalue.kt` y los DTOs.
- Si cambia el HTML de MP4Upload, actualizar la regex de `PlaybackResolver.kt`.
- Si Cloudflare empieza a bloquear también el playlist, el HLS seguirá funcionando en WebView.
- La app no guarda tokens ni credenciales; solo progreso, historial, descargas y, opcionalmente, el token de AniList en `SharedPreferences`.

## 11. AniList (opcional)

La app usa la API GraphQL pública de AniList para enriquecer metadatos y, si el usuario conecta su cuenta, sincronizar el progreso.

Endpoint:

```
POST https://graphql.anilist.co
Content-Type: application/json
Authorization: Bearer {token}   # solo para mutaciones y Viewer
```

Consulta de metadatos por MAL id:

```graphql
query ($malId: Int) {
  Media(idMal: $malId, type: ANIME) {
    id
    title { romaji english native }
    description(asHtml: false)
    coverImage { extraLarge large }
    bannerImage
    averageScore
    genres
    episodes
    status
    startDate { year }
  }
}
```

Guardar progreso:

```graphql
mutation ($mediaId: Int, $progress: Int) {
  SaveMediaListEntry(mediaId: $mediaId, progress: $progress) {
    id
    progress
  }
}
```

El flujo OAuth es implícito:

```
https://anilist.co/api/v2/oauth/authorize?client_id={client_id}&response_type=token
```

AniList redirige a `https://anilist.co/api/v2/oauth/pin#access_token=...`. El WebView intercepta esa URL y guarda el token. La app nunca incluye un Client ID propio: el usuario debe crear su app en <https://anilist.co/settings/developer> con redirect URI `https://anilist.co/api/v2/oauth/pin`.
