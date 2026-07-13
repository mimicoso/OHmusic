package com.ohmusic.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.ohmusic.app.R
import com.ohmusic.app.data.model.Song
import com.ohmusic.app.util.CoverResolver

/**
 * 歌曲卡片 — 半透明背景（无 drawBackdrop，避免与外层 layerBackdrop 循环）
 */
@Composable
fun GlassSongCard(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFavoriteToggle: (() -> Unit)? = null,
    index: Int = 0
) {
    val containerAlpha = if (isPlaying) 0.18f else 0.08f
    val cardShape = RoundedCornerShape(16.dp)
    val context = LocalContext.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(Color.White.copy(alpha = containerAlpha))
            .border(1.dp, Color.White.copy(alpha = 0.25f), cardShape)
            .clickable(
                interactionSource = null,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 封面
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(CoverResolver.resolve(song.albumId))
                    .size(96, 96)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.default_album_art),
                fallback = painterResource(R.drawable.default_album_art)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 歌曲信息
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = song.title,
                    fontSize = 15.sp,
                    fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isPlaying) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        Icons.Default.Equalizer,
                        contentDescription = stringResource(R.string.status_now_playing),
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${song.artist}  ·  ${song.album}",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 收藏按钮
        if (onFavoriteToggle != null) {
            IconButton(
                onClick = onFavoriteToggle,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = stringResource(
                        if (song.isFavorite) R.string.action_unfavorite else R.string.action_favorite
                    ),
                    tint = if (song.isFavorite) Color.White else Color.White.copy(alpha = 0.35f),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(2.dp))
        }

        // 时长
        Text(
            text = formatDuration(song.duration),
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.4f)
        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
