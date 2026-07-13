package com.ohmusic.app.ui.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.ohmusic.app.R
import com.ohmusic.app.util.CoverResolver
import com.ohmusic.app.ui.components.GlassSongCard
import com.ohmusic.app.ui.components.GlassTabBar
import kotlinx.coroutines.launch

/**
 * 歌单页 — 本地音乐列表 / 收藏列表
 *
 * 背景: 封面模糊（与播放页同步），通过 localBackdrop 捕获供 GlassTabBar 采样
 * 顶部: 液态玻璃 tab（歌单/收藏）+ 刷新/设置按钮
 * 列表: HorizontalPager 左右滑动切换
 */
@Composable
fun PlaylistScreen(
    hasAudioPermission: Boolean,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlaylistViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val allSongs = uiState.songs
    val favSongs = uiState.favoriteSongs
    val currentSong = uiState.currentSong
    val isScanning = uiState.isScanning
    val scanError = uiState.errorMessage
    val context = LocalContext.current

    // 局部 backdrop — 只捕获封面模糊背景，供 GlassTabBar 采样做液态玻璃
    val localBackdrop = rememberLayerBackdrop()

    // HorizontalPager 状态
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    // 每个进程首次进入音乐库时同步一次 MediaStore。
    LaunchedEffect(hasAudioPermission) {
        viewModel.ensureInitialScan(hasAudioPermission)
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ── 封面模糊背景 — 用 localBackdrop 捕获，供 GlassTabBar 采样 ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(localBackdrop)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(currentSong?.let { CoverResolver.resolve(it.albumId) })
                    .size(256, 256)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(48.dp),
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(2.0f) }),
                alpha = 0.9f,
                placeholder = painterResource(R.drawable.default_album_art),
                error = painterResource(R.drawable.default_album_art),
                fallback = painterResource(R.drawable.default_album_art)
            )
            // 暗色遮罩
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = statusBarTop + 12.dp)
        ) {
            // ── 顶栏：Tab + 刷新/设置 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 液态玻璃 tab — 点击切换，pager 跟随
                GlassTabBar(
                    backdrop = localBackdrop,
                    tabs = listOf(
                        stringResource(R.string.library_songs),
                        stringResource(R.string.library_favorites)
                    ),
                    selectedIndex = pagerState.currentPage,
                    onTabSelected = { index ->
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    modifier = Modifier
                        .height(44.dp)
                        .width(160.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                // 刷新按钮 — 圆形液态玻璃容器
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .drawBackdrop(
                            backdrop = localBackdrop,
                            shape = { CircleShape },
                            effects = {
                                blur(4f.dp.toPx())
                            },
                            onDrawSurface = { drawRect(Color.White.copy(alpha = 0.15f)) }
                        )
                        .clickable(
                            interactionSource = null,
                            indication = null
                        ) {
                            viewModel.refresh(
                                hasPermission = hasAudioPermission,
                                onPermissionRequired = onRequestPermission
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.action_refresh),
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }

            }

            Spacer(modifier = Modifier.height(16.dp))

            // 歌曲数量 — 跟随当前页
            val displayCount = if (pagerState.currentPage == 0) allSongs.size else favSongs.size
            Text(
                text = pluralStringResource(R.plurals.song_count, displayCount, displayCount),
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.padding(start = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── HorizontalPager：左右滑动切换歌单/收藏 ──
            if (isScanning) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = Color.White.copy(alpha = 0.6f),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.status_scanning),
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 0,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val songs = if (page == 0) allSongs else favSongs

                    if (songs.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.3f),
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = when {
                                        page != 0 -> stringResource(R.string.empty_favorites)
                                        !hasAudioPermission -> stringResource(R.string.audio_permission_required)
                                        scanError != null -> scanError.orEmpty()
                                        else -> stringResource(R.string.empty_music)
                                    },
                                    fontSize = 15.sp,
                                    color = Color.White.copy(alpha = 0.4f)
                                )
                                if (page == 0 && !hasAudioPermission) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(onClick = onRequestPermission) {
                                        Text(
                                            stringResource(R.string.action_grant_permission),
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            itemsIndexed(
                                songs,
                                key = { _, song -> song.id }
                            ) { index, song ->
                                GlassSongCard(
                                    song = song,
                                    isPlaying = currentSong?.id == song.id,
                                    onClick = {
                                        viewModel.play(song, songs)
                                    },
                                    onFavoriteToggle = {
                                        viewModel.toggleFavorite(song.id)
                                    },
                                    index = index
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
