package com.ohmusic.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * 液态玻璃底栏 — 支持拖拽 + 点击
 */
@Composable
fun SimpleBottomTabs(
    backdrop: Backdrop,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    tabLabels: List<String>,
    tabContent: @Composable (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabsCount = tabLabels.size
    require(tabsCount > 0) { "SimpleBottomTabs requires at least one tab" }
    val isLightTheme = !isSystemInDarkTheme()
    val containerColor = if (isLightTheme) Color(0xFFFAFAFA).copy(0.4f) else Color(0xFF121212).copy(0.4f)

    BoxWithConstraints(
        modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        val density = LocalDensity.current
        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val tabWidth = with(density) {
            (constraints.maxWidth.toFloat() - 8.dp.toPx()) / tabsCount
        }

        val scope = rememberCoroutineScope()
        val indicatorPos = remember { Animatable(selectedIndex.toFloat()) }
        val pressScale = remember { Animatable(1f) }
        var currentIndex by remember { mutableIntStateOf(selectedIndex) }

        // 同步外部 selectedIndex
        LaunchedEffect(selectedIndex) {
            if (selectedIndex != currentIndex) {
                currentIndex = selectedIndex
                indicatorPos.animateTo(selectedIndex.toFloat(), spring(1f, 1000f))
            }
        }

        val pressProgress = (pressScale.value - 1f) / 0.08f

        fun selectTab(target: Int) {
            currentIndex = target
            scope.launch {
                indicatorPos.animateTo(target.toFloat(), spring(1f, 1000f))
            }
            onTabSelected(target)
        }

        // ── 层1：容器壳（液态玻璃）──
        Row(
            Modifier
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
                    effects = {
                        vibrancy()
                        blur(4f.dp.toPx())
                        lens(24f.dp.toPx(), 24f.dp.toPx())
                    },
                    onDrawSurface = { drawRect(containerColor) }
                )
                .height(48.dp)
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(tabsCount) {
                Box(modifier = Modifier.weight(1f))
            }
        }

        // ── 层2：选中指示器（纯视觉 + 动画，无手势）──
        Box(
            Modifier
                .padding(horizontal = 4.dp)
                .graphicsLayer {
                    translationX =
                        if (isLtr) indicatorPos.value * tabWidth
                        else size.width - (indicatorPos.value + 1f) * tabWidth
                    scaleX = pressScale.value
                    scaleY = pressScale.value
                }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
                    effects = {
                        blur(4f.dp.toPx())
                        lens(12f.dp.toPx() * pressProgress, 12f.dp.toPx() * pressProgress)
                    },
                    highlight = {
                        Highlight.Default.copy(alpha = pressProgress)
                    },
                    shadow = {
                        Shadow(alpha = 0.2f + 0.1f * pressProgress)
                    },
                    innerShadow = {
                        InnerShadow(
                            radius = lerp(4f, 8f, pressProgress).dp,
                            alpha = 0.15f + 0.05f * pressProgress
                        )
                    },
                    onDrawSurface = {
                        drawRect(Color.White.copy(alpha = lerp(0.12f, 0.2f, pressProgress)))
                    }
                )
                .height(40.dp)
                .fillMaxWidth(1f / tabsCount)
        )

        // ── 层3：纯图标显示 ──
        Row(
            Modifier
                .height(48.dp)
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(tabsCount) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .selectable(
                            selected = currentIndex == index,
                            role = Role.Tab,
                            onClick = { selectTab(index) }
                        )
                        .semantics {
                            contentDescription = tabLabels[index]
                        },
                    contentAlignment = Alignment.Center
                ) {
                    tabContent(index, currentIndex == index)
                }
            }
        }

        // ── 层4：全宽透明手势层（覆盖整条底栏）──
        Box(
            Modifier
                .height(48.dp)
                .fillMaxWidth()
                .padding(4.dp)
                .pointerInput(tabsCount, tabWidth, currentIndex) {
                    val dragThreshold = 10f
                    val velocityTracker = VelocityTracker()

                    awaitEachGesture {
                        val down = awaitFirstDown()
                        var isDragging = false
                        val startX = down.position.x
                        val startIndex = currentIndex
                        velocityTracker.resetTracking()
                        velocityTracker.addPosition(down.uptimeMillis, down.position)

                        scope.launch {
                            pressScale.animateTo(1.08f, spring(1f, 1000f))
                        }

                        drag(down.id) { change ->
                            val dx = change.position.x - startX
                            velocityTracker.addPosition(change.uptimeMillis, change.position)

                            if (!isDragging && abs(dx) > dragThreshold) {
                                isDragging = true
                            }

                            if (isDragging) {
                                val delta = if (isLtr) dx else -dx
                                val newValue = (startIndex.toFloat() + delta / tabWidth)
                                    .fastCoerceIn(0f, (tabsCount - 1).toFloat())
                                scope.launch { indicatorPos.snapTo(newValue) }
                            }
                            change.consume()
                        }

                        val target = if (isDragging) {
                            val velocity = if (isLtr) {
                                velocityTracker.calculateVelocity().x
                            } else {
                                -velocityTracker.calculateVelocity().x
                            }
                            val flingDelta = velocity / tabWidth * 0.3f
                            (indicatorPos.value + flingDelta).fastRoundToInt()
                                .fastCoerceIn(0, tabsCount - 1)
                        } else {
                            if (isLtr) {
                                (down.position.x / tabWidth).toInt()
                            } else {
                                (tabsCount - 1 - down.position.x / tabWidth).toInt()
                            }.fastCoerceIn(0, tabsCount - 1)
                        }

                        selectTab(target)
                        scope.launch {
                            pressScale.animateTo(1f, spring(1f, 800f))
                        }
                    }
                }
        )
    }
}
