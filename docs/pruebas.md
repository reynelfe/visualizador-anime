# Plan de pruebas

## 1. Emulador Android TV

Crear un AVD de TV (si tienes la imagen instalada):

```bash
sdkmanager "system-images;android-34;android-tv;x86_64"
avdmanager create avd -n AnimeAVTV -k "system-images;android-34;android-tv;x86_64" -d tv_1080p
emulator -avd AnimeAVTV -no-snapshot -gpu swiftshader_indirect
```

Instalar y abrir:

```bash
./scripts/install.sh
```

## 2. Pruebas funcionales

| # | Caso | Resultado esperado |
| --- | --- | --- |
| 1 | Abrir la app | Home con hero, filas y barra superior. |
| 2 | Navegar con D-pad | El foco se ve en tarjetas, chips y botones; las filas se desplazan. |
| 3 | Abrir un anime | Ficha con sinopsis, géneros, relaciones y episodios. |
| 4 | Pulsar un episodio | Se abre el reproductor nativo o web según el servidor. |
| 5 | Reproductor MP4Upload | Video reproduce, se puede pausar y hacer seek. |
| 6 | Reproductor HLS | El iframe de zilla carga y reproduce. |
| 7 | Cambiar servidor | El botón “Servidor” del reproductor muestra la lista y recarga la fuente. |
| 8 | Volver al detalle | Aparece “Continuar EP N” con el progreso guardado. |
| 9 | Mi lista | Guardar y quitar un anime; persiste tras reiniciar la app. |
| 10 | Historial | “Continuar viendo” en Home y “Borrar historial” en Ajustes. |
| 11 | Catálogo | Filtros por género, categoría, año y orden; paginación al bajar. |
| 12 | Buscador | Escribir “one piece” y abrir un resultado. |
| 13 | Ajustes | Cambiar servidor preferido y calidad; se guarda. |
| 14 | Sin red | Mensaje de error con botón “Reintentar”. |

## 3. Pruebas de regresión

- Cambiar el `User-Agent` y comprobar que AnimeAV1 sigue respondiendo.
- Probar un anime con muchos episodios (One Piece, 1180) y comprobar que el grid no bloquea la UI.
- Probar un anime sin episodios y comprobar el mensaje vacío.
- Probar un episodio sin embed MP4Upload y comprobar el fallback a WebView.
- Probar un servidor caído y comprobar el mensaje de error del reproductor.

## 4. Logs

```bash
adb logcat -s AnimeAVTV:V AndroidRuntime:E
```
