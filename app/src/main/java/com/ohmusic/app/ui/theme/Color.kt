package com.ohmusic.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── Brand Colors (蓝色主题) ──
val BlueDeep = Color(0xFF0A1628)           // 主背景最深
val BlueMid = Color(0xFF122244)            // 中间蓝
val BlueLight = Color(0xFF3B82F6)          // 亮蓝 accent
val BlueAccent = Color(0xFF60A5FA)         // 天蓝高光
val BlueGlow = Color(0xFF93C5FD)           // 浅蓝光晕

// 兼容旧引用（逐步迁移后可删除）
val PurpleLight = BlueLight

// ── Background Gradients ──
val BgTop = Color(0xFF0F1D35)              // 背景顶部
val BgMid = Color(0xFF0A1628)              // 背景中部
val BgBottom = Color(0xFF060E1A)           // 背景底部

// ── Surface ──
val SurfaceDark = Color(0xFF111D33)        // 卡片/面板
val SurfaceOverlay = Color(0x40FFFFFF)     // 玻璃叠层

// ── Text ──
val TextPrimary = Color(0xFFFFFFFF)        // 主文字
val TextSecondary = Color(0xFF94A3B8)      // 辅助文字（蓝灰）
val TextTertiary = Color(0xFF64748B)       // 弱文字

// ── Controls ──
val ProgressActive = Color(0xFFFFFFFF)     // 进度条激活
val ProgressInactive = Color(0x40FFFFFF)   // 进度条未激活
val IconActive = Color(0xFFFFFFFF)         // 图标激活
val IconInactive = Color(0x80FFFFFF)       // 图标未激活
