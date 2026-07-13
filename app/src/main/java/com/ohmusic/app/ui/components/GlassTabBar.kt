package com.ohmusic.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule

/**
 * 液态玻璃分段切换 — 参考官方 LiquidBottomTabs 三层架构
 *
 * 第1层: 外层容器 — drawBackdrop(backdrop) 液态玻璃背景
 * 第2层: 捕获层 — layerBackdrop(tabsBackdrop) 捕获外层效果
 * 第3层: 选中指示器 — drawBackdrop(rememberCombinedBackdrop) 同时采样背景+容器
 * 第4层: 文字层 — 始终在最上层，确保可见
 */
@Composable
fun GlassTabBar(
    backdrop: Backdrop,
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isLightTheme = !isSystemInDarkTheme()
    val containerColor =
        if (isLightTheme) Color(0xFFFAFAFA).copy(0.35f)
        else Color(0xFF121212).copy(0.35f)
    val accentColor =
        if (isLightTheme) Color.White
        else Color.White

    // 捕获层 backdrop
    val tabsBackdrop = rememberLayerBackdrop()

    BoxWithConstraints(
        modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        val density = LocalDensity.current
        val tabCount = tabs.size
        val tabWidthPx = with(density) { (constraints.maxWidth.toFloat() - 8.dp.toPx()) / tabCount }

        // 选中指示器的水平偏移动画
        val indicatorOffset by animateFloatAsState(
            targetValue = selectedIndex.toFloat(),
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
            label = "indicatorOffset"
        )

        // ── 第1层: 外层容器（柔和背景）──
        Row(
            Modifier
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
                    effects = {
                        blur(4f.dp.toPx())
                    },
                    onDrawSurface = { drawRect(containerColor.copy(alpha = 0.15f)) }
                )
                .height(44.dp)
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 空的占位，保持结构
            tabs.forEach { _ ->
                Box(modifier = Modifier.weight(1f).height(36.dp))
            }
        }

        // ── 第2层: 捕获层（不可见）──
        Row(
            Modifier
                .alpha(0f)
                .layerBackdrop(tabsBackdrop)
                .height(44.dp)
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { _ ->
                Box(modifier = Modifier.weight(1f).height(36.dp))
            }
        }

        // ── 第3层: 选中指示器 ──
        Box(
            Modifier
                .offset {
                    IntOffset(
                        x = (indicatorOffset * tabWidthPx + 4.dp.toPx()).toInt(),
                        y = 0
                    )
                }
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                    shape = { Capsule() },
                    effects = {
                        vibrancy()
                        blur(6f.dp.toPx())
                        lens(16f.dp.toPx(), 20f.dp.toPx(), chromaticAberration = true)
                    },
                    shadow = { Shadow(alpha = 0.4f) },
                    innerShadow = { InnerShadow(radius = 6.dp, alpha = 0.3f) },
                    onDrawSurface = {
                        drawRect(Color.White.copy(0.15f))
                    }
                )
                .height(36.dp)
                .width(with(density) { tabWidthPx.toDp() })
        )

        // ── 第4层: 文字层（最上层，确保可见）──
        Row(
            Modifier
                .height(44.dp)
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, text ->
                val selected = selectedIndex == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .selectable(
                            selected = selected,
                            role = Role.Tab,
                            onClick = { onTabSelected(index) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = text,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) accentColor else Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
