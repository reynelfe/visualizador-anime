@file:OptIn(ExperimentalFoundationApi::class)

package com.animeav.tv.ui.components

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.animeav.tv.ui.theme.AnimeRed
import com.animeav.tv.ui.theme.AnimeRedSoft
import com.animeav.tv.ui.theme.Background
import com.animeav.tv.ui.theme.SurfaceElevated
import com.animeav.tv.ui.theme.TextSecondary

/**
 * Al desplazar una fila con el D-pad en TV no queremos tarjetas a medias en el
 * borde derecho. Esta especificación solo desplaza lo justo para que el elemento
 * enfocado quede completamente visible y sin animación.
 */
private object RowBringIntoViewSpec : BringIntoViewSpec {
    override val scrollAnimationSpec: AnimationSpec<Float> = snap()

    override fun calculateScrollDistance(offset: Float, size: Float, viewportSize: Float): Float {
        val end = offset + size
        return when {
            offset < 0f -> offset
            end > viewportSize -> end - viewportSize
            else -> 0f
        }
    }
}

/**
 * Fila horizontal que reparte el ancho disponible en un número entero de
 * tarjetas, de modo que nunca se vea una tarjeta cortada en el borde.
 */
@Composable
fun FitRow(
    minItemWidth: Dp,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 42.dp,
    spacing: Dp = 8.dp,
    content: LazyListScope.(itemWidth: Dp) -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
    ) {
        val density = LocalDensity.current
        val spacingPx = with(density) { spacing.roundToPx() }
        val minPx = with(density) { minItemWidth.roundToPx() }
        val availablePx = constraints.maxWidth
        val count = ((availablePx + spacingPx) / (minPx + spacingPx)).coerceAtLeast(1)
        val itemPx = ((availablePx - spacingPx * (count - 1)) / count).coerceAtLeast(1)
        val itemWidth = with(density) { itemPx.toDp() }

        CompositionLocalProvider(LocalBringIntoViewSpec provides RowBringIntoViewSpec) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                content(itemWidth)
            }
        }
    }
}

/**
 * Suaviza el borde de una fila desplazable (por ejemplo, los chips de filtros)
 * para que el contenido no termine a hueso contra el borde de la pantalla.
 */
@Composable
fun FadeRow(
    state: LazyListState,
    modifier: Modifier = Modifier,
    fade: Dp = 48.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.drawWithContent {
            drawContent()
            val fadePx = fade.toPx().coerceAtMost(size.width / 2f)
            if (fadePx <= 1f) return@drawWithContent
            if (state.canScrollForward) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        0f to Color.Transparent,
                        1f to Background,
                        startX = size.width - fadePx,
                        endX = size.width,
                    ),
                    topLeft = Offset(size.width - fadePx, 0f),
                    size = Size(fadePx, size.height),
                )
            }
            if (state.canScrollBackward) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        0f to Background,
                        1f to Color.Transparent,
                        startX = 0f,
                        endX = fadePx,
                    ),
                    size = Size(fadePx, size.height),
                )
            }
        },
    ) {
        content()
    }
}

@Composable
fun rememberImageRequest(url: String, widthPx: Int, heightPx: Int): ImageRequest {
    val context = LocalContext.current
    return remember(url, widthPx, heightPx) {
        ImageRequest.Builder(context)
            .data(url)
            .size(widthPx, heightPx)
            .precision(Precision.INEXACT)
            .crossfade(false)
            .build()
    }
}

@Composable
fun BrandLogo(modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(AnimeRed),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = "ANIME",
            color = AnimeRed,
            fontWeight = FontWeight.Black,
            fontSize = 24.sp,
            letterSpacing = (-1).sp,
        )
        Text(
            text = "AV",
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Black,
            fontSize = 24.sp,
            letterSpacing = (-1).sp,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "TV",
            color = TextSecondary,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
fun NavTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val color = if (selected) AnimeRed
    else if (focused) MaterialTheme.colorScheme.onBackground
    else TextSecondary
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (focused) SurfaceElevated else Color.Transparent)
            .onFocusChanged { focused = it.isFocused }
            .clickable(
                interactionSource = null,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Text(label, color = color, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .width(22.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (selected) AnimeRed else Color.Transparent),
        )
    }
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            FocusIconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
            }
            Spacer(Modifier.width(20.dp))
        }
        Column {
            Text(title, style = MaterialTheme.typography.headlineLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
        }
    }
}

@Composable
fun FocusIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(50))
            .background(if (focused) AnimeRed else SurfaceElevated)
            .onFocusChanged { focused = it.isFocused }
            .clickable(
                interactionSource = null,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = AnimeRed,
            contentColor = Color.White,
        ),
        shape = RoundedCornerShape(10.dp),
    ) {
        if (icon != null) {
            icon()
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = SurfaceElevated,
            contentColor = MaterialTheme.colorScheme.onBackground,
        ),
        shape = RoundedCornerShape(10.dp),
    ) {
        if (icon != null) {
            icon()
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.width(16.dp))
        Box(
            Modifier
                .height(3.dp)
                .width(44.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(AnimeRed),
        )
        if (action != null && onAction != null) {
            Spacer(Modifier.weight(1f))
            Text(
                action,
                color = TextSecondary,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        onClick = onAction,
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
fun PosterCard(
    title: String,
    imageUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    badge: String? = null,
    width: androidx.compose.ui.unit.Dp = 178.dp,
) {
    var focused by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val imageWidth = with(density) { width.roundToPx() }
    val imageRequest = rememberImageRequest(imageUrl, imageWidth, imageWidth * 3 / 2)

    Column(
        modifier = modifier
            .width(width)
            .onFocusChanged { focused = it.isFocused }
            .clickable(
                interactionSource = null,
                indication = null,
                onClick = onClick,
            )
            .padding(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceElevated)
                .drawWithContent {
                    drawContent()
                    if (focused) {
                        drawRoundRect(
                            color = AnimeRed,
                            style = Stroke(width = 3.dp.toPx()),
                            cornerRadius = CornerRadius(12.dp.toPx()),
                        )
                    }
                },
        ) {
            AsyncImage(
                model = imageRequest,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (badge != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(AnimeRed)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(badge, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun EpisodeWideCard(
    title: String,
    episodeNumber: Int,
    imageUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float = 0f,
    subtitle: String? = null,
    width: Dp = 300.dp,
) {
    var focused by remember { mutableStateOf(false) }
    val imageRequest = rememberImageRequest(imageUrl, 600, 338)

    Column(
        modifier = modifier
            .width(width)
            .onFocusChanged { focused = it.isFocused }
            .clickable(
                interactionSource = null,
                indication = null,
                onClick = onClick,
            )
            .padding(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceElevated)
                .drawWithContent {
                    drawContent()
                    if (focused) {
                        drawRoundRect(
                            color = AnimeRed,
                            style = Stroke(width = 3.dp.toPx()),
                            cornerRadius = CornerRadius(10.dp.toPx()),
                        )
                    }
                },
        ) {
            AsyncImage(
                model = imageRequest,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                        ),
                    ),
            )
            Text(
                text = "EP $episodeNumber",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp),
            )
            if (progress > 0f) {
                LinearProgressIndicator(
                    progress = { progress },
                    color = AnimeRed,
                    trackColor = Color.White.copy(alpha = 0.25f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(4.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun EpisodeNumberCard(
    number: Int,
    title: String?,
    current: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (current) AnimeRed.copy(alpha = 0.22f) else SurfaceElevated)
            .drawWithContent {
                drawContent()
                if (focused) {
                    drawRoundRect(
                        color = AnimeRed,
                        style = Stroke(width = 2.dp.toPx()),
                        cornerRadius = CornerRadius(10.dp.toPx()),
                    )
                }
            }
            .onFocusChanged { focused = it.isFocused }
            .clickable(
                interactionSource = null,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = "EP $number",
            color = if (current) AnimeRedSoft else Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        )
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun LoadingState(message: String = "Cargando…") {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = AnimeRed)
            Spacer(Modifier.height(18.dp))
            Text(message, color = TextSecondary)
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(48.dp),
        ) {
            Icon(
                Icons.Filled.Movie,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text("No se pudo cargar", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                message,
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(20.dp))
            PrimaryButton("Reintentar", onRetry)
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = TextSecondary, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun StaticChip(
    text: String,
    highlighted: Boolean = false,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (highlighted) AnimeRed else Color(0xFF252531))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text,
            color = if (highlighted) Color.White else TextSecondary,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
fun Chip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val background = when {
        selected -> AnimeRed
        focused -> SurfaceElevated
        else -> Color(0xFF1B1B25)
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .border(
                width = if (focused && !selected) 2.dp else 0.dp,
                color = if (focused && !selected) AnimeRed else Color.Transparent,
                shape = RoundedCornerShape(50),
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(
                interactionSource = null,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Text(
            text,
            color = if (selected || focused) Color.White else TextSecondary,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
fun HorizontalGradientScrim(modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(
                Brush.horizontalGradient(
                    0f to Color.Black.copy(alpha = 0.92f),
                    0.55f to Color.Black.copy(alpha = 0.55f),
                    1f to Color.Transparent,
                ),
            ),
    )
}

@Composable
fun VerticalGradientScrim(modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.55f to Background.copy(alpha = 0.25f),
                    1f to Background,
                ),
            ),
    )
}
