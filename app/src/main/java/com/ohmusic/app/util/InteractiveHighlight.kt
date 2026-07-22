package com.ohmusic.app.util

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.spring
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Size
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 触摸跟随高光效果
 * hasOwnGesture = true: 独立版（自带手势，适用于独立按钮）
 * hasOwnGesture = false: catalog版（无手势，配合 DampedDragAnimation 使用）
 */
class InteractiveHighlight(
    private val animationScope: CoroutineScope,
    private val position: (size: Size, offset: Offset) -> Offset = { _, _ -> Offset.Zero },
    private val hasOwnGesture: Boolean = true
) {
    companion object {
        private val OffsetToVector: TwoWayConverter<Offset, AnimationVector2D> =
            TwoWayConverter(
                convertToVector = { AnimationVector2D(it.x, it.y) },
                convertFromVector = { Offset(it.v1, it.v2) }
            )
    }

    // 按下：快速响应
    private val pressDownSpec = spring<Float>(1f, 1200f, 0.001f)
    // 松手光斑消失：慢于位置回弹，保持可见
    private val releaseSpec = spring<Float>(0.85f, 350f, 0.001f)
    // 位置回弹：自然弹性
    private val positionBounceSpec = spring<Offset>(0.45f, 500f, Offset(0.5f, 0.5f))

    private val pressProgressAnimation = Animatable(0f, 0.001f)
    private val positionAnimation = Animatable(Offset.Zero, OffsetToVector)
    private var startPosition = Offset.Zero

    val pressProgress: Float get() = pressProgressAnimation.value
    val offset: Offset get() = positionAnimation.value - startPosition

    /** 视觉高光 — 绘制在内容之上，BlendMode.Plus */
    val modifier: Modifier = Modifier.drawWithContent {
        val progress = pressProgressAnimation.value
        if (progress > 0f) {
            drawRect(Color.White.copy(0.18f * progress), blendMode = BlendMode.Plus)
            val pos = position(size, positionAnimation.value)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(0.5f * progress),
                        Color.White.copy(0.18f * progress),
                        Color.Transparent
                    ),
                    center = pos,
                    radius = size.minDimension * 1.8f
                ),
                blendMode = BlendMode.Plus
            )
        }
        drawContent()
    }

    /** 手势捕获 — hasOwnGesture=false 时为空（配合 DampedDragAnimation） */
    val gestureModifier: Modifier = Modifier

    fun press() {
        animationScope.launch {
            pressProgressAnimation.snapTo(1f)
        }
    }

    fun release() {
        animationScope.launch {
            pressProgressAnimation.animateTo(0f, releaseSpec)
        }
    }
}
