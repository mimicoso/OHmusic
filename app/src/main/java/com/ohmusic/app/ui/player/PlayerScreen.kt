package com.ohmusic.app.ui.player

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.ohmusic.app.R
import com.ohmusic.app.data.model.Song
import com.ohmusic.app.util.CoverResolver
import com.ohmusic.app.player.PlayMode
import com.ohmusic.app.ui.theme.ProgressActive
import com.ohmusic.app.ui.theme.ProgressInactive
import com.ohmusic.app.ui.theme.TextPrimary
import com.ohmusic.app.util.InteractiveHighlight
import kotlinx.coroutines.CoroutineScope

/**
 * 播放页 — Spotify/Apple Music 同款方案
 * 架构（同 cool-digital 的 DiscoverScreen）：
 * - Column 内容：layerBackdrop source（封面、歌曲信息、进度条）
 * - PlaybackGlassControls：sibling overlay，用真正的 drawBackdrop 折射封面内容
 */
@Composable
fun PlayerScreen(
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val backdrop = rememberLayerBackdrop()
    val uiState by viewModel.uiState.collectAsState()
    val currentSong = uiState.currentSong
    val isPlaying = uiState.isPlaying
    val progress = uiState.progress
    val currentPosition = uiState.currentPosition
    val duration = uiState.duration
    val playMode = uiState.playMode
    val isFavorite = uiState.isFavorite
    val playModeLabel = stringResource(
        when (playMode) {
            PlayMode.SEQUENCE -> R.string.play_mode_sequence
            PlayMode.REPEAT_ONE -> R.string.play_mode_repeat_one
            PlayMode.SHUFFLE -> R.string.play_mode_shuffle
        }
    )

    // 首帧完成后再连接播放服务，避免 ExoPlayer 初始化竞争冷启动关键路径。
    LaunchedEffect(Unit) {
        withFrameNanos { }
        viewModel.connect()
    }

    val artUri = currentSong?.let { CoverResolver.resolve(it.albumId) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ── backdrop 采样源：模糊背景+内容（不含播放控件）──
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
        ) {
            // ── 封面模糊背景（Crossfade 过渡，切歌不硬切）──
            Crossfade(
                targetState = artUri,
                animationSpec = tween(1200),
                label = "bg"
            ) { uri ->
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(uri)
                        .size(512, 512)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(64.dp),
                    contentScale = ContentScale.Crop,
                    colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(2.0f) }),
                    alpha = 0.9f,
                    placeholder = painterResource(R.drawable.default_album_art),
                    error = painterResource(R.drawable.default_album_art),
                    fallback = painterResource(R.drawable.default_album_art)
                )
            }
            // 暗色遮罩
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            )

            // ── 内容 Column ──
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 36.dp)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(top = 12.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            // 封面
            AlbumArtCard(
                song = currentSong,
                isPlaying = isPlaying,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 歌曲信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                    Text(
                        text = currentSong?.title ?: stringResource(R.string.status_not_playing),
                        fontSize = 26.sp, fontWeight = FontWeight.Bold,
                        color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentSong?.artist ?: "",
                        fontSize = 15.sp, color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = viewModel::togglePlayMode,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = when (playMode) {
                            PlayMode.SEQUENCE -> Icons.Default.Repeat
                            PlayMode.REPEAT_ONE -> Icons.Default.RepeatOne
                            PlayMode.SHUFFLE -> Icons.Default.Shuffle
                        },
                        contentDescription = stringResource(
                            R.string.action_change_play_mode,
                            playModeLabel
                        ),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(
                    onClick = viewModel::toggleFavorite,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = stringResource(
                            if (isFavorite) R.string.action_unfavorite else R.string.action_favorite
                        ),
                        tint = if (isFavorite) Color.White else Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 进度条
            Canvas(
                modifier = Modifier.fillMaxWidth().height(24.dp).pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        viewModel.seekTo((down.position.x / size.width).coerceIn(0f, 1f))
                        down.consume()
                        drag(down.id) { change ->
                            viewModel.seekTo((change.position.x / size.width).coerceIn(0f, 1f))
                            change.consume()
                        }
                    }
                }
            ) {
                val trackHeight = 2.dp.toPx()
                val dotRadius = 5.dp.toPx()
                val y = size.height / 2f
                val w = size.width
                val cornerRadius = 1.dp.toPx()

                drawRoundRect(
                    ProgressInactive, Offset(0f, y - trackHeight / 2f),
                    Size(w, trackHeight), CornerRadius(cornerRadius)
                )
                val activeWidth = w * progress
                drawRoundRect(
                    ProgressActive, Offset(0f, y - trackHeight / 2f),
                    Size(activeWidth, trackHeight), CornerRadius(cornerRadius)
                )
                drawCircle(ProgressActive, dotRadius, Offset(activeWidth, y))
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(currentPosition), fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                Text(formatTime(duration), fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
            }

            Spacer(modifier = Modifier.weight(1f))
        }
        } // backdrop source wrapper

        // ── 播放控件 — sibling overlay，居中定位 ──
        PlaybackGlassControls(
            backdrop = backdrop,
            isPlaying = isPlaying,
            onPrevious = viewModel::previous,
            onTogglePlay = viewModel::togglePlay,
            onNext = viewModel::next,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 128.dp)
                .fillMaxWidth()
                .padding(horizontal = 48.dp)
        )
    }
}

/** 专辑封面卡片 — 外发光 */
@Composable
private fun AlbumArtCard(
    song: Song?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val cornerRadius = 20.dp

    Box(
        modifier = modifier
            .graphicsLayer {
                // 外发光：shadowElevation + shape
                shadowElevation = if (isPlaying) 24f else 12f
                shape = RoundedCornerShape(cornerRadius)
                clip = false
                ambientShadowColor = Color.White
                spotShadowColor = Color.White
            }
    ) {
        // 封面卡片
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(cornerRadius))
                .drawBehind {
                    val strokeW = 2.5f.dp.toPx()
                    val r = cornerRadius.toPx()
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.25f),
                                Color.White.copy(alpha = 0.08f),
                                Color.White.copy(alpha = 0.3f)
                            )
                        ),
                        cornerRadius = CornerRadius(r),
                        style = Stroke(width = strokeW)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = song?.let { CoverResolver.resolve(it.albumId) },
                contentDescription = song?.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.default_album_art),
                error = painterResource(R.drawable.default_album_art),
                fallback = painterResource(R.drawable.default_album_art)
            )
        }
    }
}

/**
 * 播放控件 — 严格按 AndroidLiquidGlass 库规范
 * drawBackdrop(backdrop) + CircleShape + vibrancy/blur/lens
 * + InteractiveHighlight 触摸高光 + layerBlock 按压缩放
 */
@Composable
private fun PlaybackGlassControls(
    backdrop: Backdrop,
    isPlaying: Boolean,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animationScope = rememberCoroutineScope()

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassButton(backdrop, 48.dp, onPrevious, animationScope) {
            Icon(
                Icons.Default.SkipPrevious,
                stringResource(R.string.action_previous),
                tint = TextPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(24.dp))
        GlassButton(backdrop, 72.dp, onTogglePlay, animationScope) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                stringResource(if (isPlaying) R.string.action_pause else R.string.action_play),
                tint = TextPrimary, modifier = Modifier.size(34.dp)
            )
        }
        Spacer(Modifier.width(24.dp))
        GlassButton(backdrop, 48.dp, onNext, animationScope) {
            Icon(
                Icons.Default.SkipNext,
                stringResource(R.string.action_next),
                tint = TextPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * 单个玻璃按钮 — 严格按库规范:
 * 1. drawBackdrop(backdrop, shape={CircleShape}, effects={vibrancy+blur+lens})
 * 2. highlight/shadow/innerShadow 随 pressProgress 变化
 * 3. layerBlock: scaleX/scaleY 按压缩放
 * 4. onDrawSurface: 半透明表面
 * 5. clickable + InteractiveHighlight.modifier + gestureModifier
 */
@Composable
private fun GlassButton(
    backdrop: Backdrop,
    btnSize: Dp,
    onClick: () -> Unit,
    animationScope: CoroutineScope,
    content: @Composable () -> Unit
) {
    val highlight = remember(animationScope) { InteractiveHighlight(animationScope = animationScope) }
    val p = highlight.pressProgress

    Box(
        modifier = Modifier
            .size(btnSize)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { CircleShape },
                effects = {
                    vibrancy()
                    blur(4f.dp.toPx())
                    lens(12f.dp.toPx(), 18f.dp.toPx(), chromaticAberration = true)
                },
                highlight = {
                    Highlight.Default.copy(alpha = 0.5f + 0.4f * p)
                },
                shadow = {
                    Shadow(alpha = 0.35f + 0.3f * p)
                },
                innerShadow = {
                    InnerShadow(radius = 4f.dp + 3f.dp * p, alpha = 0.25f + 0.35f * p)
                },
                layerBlock = {
                    scaleX = 1f - 0.06f * p
                    scaleY = 1f - 0.06f * p
                    val off = highlight.offset
                    translationX = btnSize.toPx() * 0.015f * (off.x / btnSize.toPx() - 0.5f)
                    translationY = btnSize.toPx() * 0.015f * (off.y / btnSize.toPx() - 0.5f)
                },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.15f)) }
            )
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .then(highlight.modifier)
            .then(highlight.gestureModifier),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
