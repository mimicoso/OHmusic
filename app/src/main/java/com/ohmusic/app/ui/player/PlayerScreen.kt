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
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.TextUnit
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
import androidx.compose.material.icons.filled.Delete

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.text.TextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
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
import android.content.Context
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
import com.ohmusic.app.util.LyricLine
import com.ohmusic.app.util.LyricParser

import com.ohmusic.app.player.PlayMode
import com.ohmusic.app.ui.theme.ProgressActive
import com.ohmusic.app.ui.theme.ProgressInactive
import com.ohmusic.app.ui.theme.TextPrimary
import com.ohmusic.app.util.InteractiveHighlight
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay

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

    var showSongInfo by rememberSaveable { mutableStateOf(false) }

    // 首帧完成后再连接播放服务，避免 ExoPlayer 初始化竞争冷启动关键路径。
    LaunchedEffect(Unit) {
        withFrameNanos { }
        viewModel.connect()
    }

    LaunchedEffect(currentSong) {
        if (currentSong != null) {
            viewModel.loadLyrics(currentSong)
        }
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
                    .padding(top = 48.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            // 封面
            AlbumArtCard(
                song = currentSong,
                isPlaying = isPlaying,
                onClick = { showSongInfo = !showSongInfo },
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                showLyrics = showSongInfo,
                lyrics = uiState.lyrics,
                currentPosition = currentPosition
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
        )
    }
}

/** 专辑封面卡片 — 外发光 */
@Composable
private fun AlbumArtCard(
    song: Song?,
    isPlaying: Boolean,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    showLyrics: Boolean = false,
    lyrics: List<LyricLine> = emptyList(),
    currentPosition: Long = 0L
) {
    val cornerRadius = 20.dp

    Box(
        modifier = modifier
            .graphicsLayer {
                shadowElevation = if (isPlaying) 24f else 12f
                shape = RoundedCornerShape(cornerRadius)
                clip = false
                ambientShadowColor = Color.White
                spotShadowColor = Color.White
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
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

        // 歌词覆盖层（只在封面范围内显示）
        if (showLyrics) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                if (lyrics.isEmpty()) {
                    Text(
                        text = "No lyrics found",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                } else {
                    val currentLineIndex = LyricParser.findCurrentLineIndex(lyrics, currentPosition)
                    val listState = rememberLazyListState()

                    LaunchedEffect(currentLineIndex) {
                        if (currentLineIndex >= 0) {
                            listState.animateScrollToItem(
                                index = currentLineIndex,
                                scrollOffset = -listState.layoutInfo.viewportSize.height / 2
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 32.dp),
                        state = listState,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        itemsIndexed(lyrics) { index, line ->
                            val isCurrent = index == currentLineIndex
                            val isNear = kotlin.math.abs(index - currentLineIndex) <= 2

                            MarqueeText(
                                text = line.text,
                                fontSize = if (isCurrent) 16.sp else 12.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = when {
                                    isCurrent -> Color.White
                                    isNear -> Color.White.copy(alpha = 0.6f)
                                    else -> Color.White.copy(alpha = 0.3f)
                                },
                                lineHeight = if (isCurrent) 24.sp else 18.sp,
                                modifier = Modifier.padding(vertical = 3.dp),
                                isCurrent = isCurrent
                            )
                        }
                    }
                }
            }
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

    Box(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.Center,
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
            Spacer(modifier = Modifier.width(48.dp))
            GlassButton(backdrop, 72.dp, onTogglePlay, animationScope) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    stringResource(if (isPlaying) R.string.action_pause else R.string.action_play),
                    tint = TextPrimary, modifier = Modifier.size(34.dp)
                )
            }
            Spacer(modifier = Modifier.width(48.dp))
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
    val interactionSource = remember { MutableInteractionSource() }

    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressed) {
        if (isPressed) {
            highlight.press()
        } else {
            highlight.release()
        }
    }

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
                },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.15f)) }
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .then(highlight.modifier),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun SettingsDialog(
    onDismiss: () -> Unit,
    backdrop: Backdrop
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("ohmusic_prefs", Context.MODE_PRIVATE)
    val excludedFolders = remember {
        mutableStateListOf<String>()
    }.apply {
        val saved = prefs.getString("excluded_folders", "")
        if (!saved.isNullOrEmpty()) {
            clear()
            addAll(saved.split("||").filter { it.isNotEmpty() })
        }
    }
    val newFolder = remember { mutableStateOf("") }

    val saveAndDismiss = {
        prefs.edit().putString("excluded_folders", excludedFolders.joinToString("||")).apply()
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(onClick = saveAndDismiss),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(320.dp)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(20.dp) },
                    effects = {
                        blur(8f.dp.toPx())
                        vibrancy()
                    },
                    onDrawSurface = { drawRect(Color.White.copy(alpha = 0.15f)) }
                )
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = stringResource(R.string.settings_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                Text(
                    text = stringResource(R.string.settings_folder_filter),
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (excludedFolders.isEmpty()) {
                    Text(
                        text = stringResource(R.string.settings_no_folders),
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .height(120.dp)
                            .padding(bottom = 12.dp)
                    ) {
                        items(excludedFolders.size) { index ->
                            val folder = excludedFolders[index]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = folder,
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable(onClick = { excludedFolders.removeAt(index) }),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.settings_remove_folder),
                                        tint = Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.OutlinedTextField(
                        value = newFolder.value,
                        onValueChange = { newFolder.value = it },
                        placeholder = {
                            Text(
                                stringResource(R.string.settings_add_folder),
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.4f)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        textStyle = TextStyle(fontSize = 13.sp, color = Color.White),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White.copy(alpha = 0.3f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedLabelColor = Color.White.copy(alpha = 0.8f),
                            cursorColor = Color.White
                        ),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    androidx.compose.material3.Button(
                        onClick = {
                            val folder = newFolder.value.trim()
                            if (folder.isNotEmpty() && folder !in excludedFolders) {
                                excludedFolders.add(folder)
                                newFolder.value = ""
                            }
                        },
                        modifier = Modifier.height(44.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            stringResource(R.string.settings_add_folder),
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                androidx.compose.material3.Button(
                    onClick = saveAndDismiss,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        stringResource(R.string.settings_save),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}



private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

@Composable
private fun MarqueeText(
    text: String,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    color: Color,
    lineHeight: TextUnit,
    modifier: Modifier = Modifier,
    isCurrent: Boolean = false
) {
    val textWidth = remember { mutableStateOf(0) }
    val containerWidth = remember { mutableStateOf(0) }
    val scrollState = rememberScrollState()

    LaunchedEffect(isCurrent, textWidth.value, containerWidth.value) {
        if (isCurrent && textWidth.value > containerWidth.value && containerWidth.value > 0) {
            while (isCurrent) {
                    scrollState.animateScrollTo(scrollState.maxValue, animationSpec = tween(2500))
                    delay(500)
                    scrollState.animateScrollTo(0, animationSpec = tween(2500))
                    delay(500)
                }
        } else {
            scrollState.scrollTo(0)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                containerWidth.value = coordinates.size.width
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = color,
            lineHeight = lineHeight,
            modifier = Modifier.horizontalScroll(scrollState),
            textAlign = TextAlign.Center,
            softWrap = false,
            maxLines = 1,
            overflow = TextOverflow.Visible,
            onTextLayout = { layoutResult ->
                textWidth.value = layoutResult.size.width
            }
        )
    }
}
