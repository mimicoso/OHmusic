package com.ohmusic.app.player

import androidx.media3.common.Player

internal fun nextPlayMode(current: PlayMode): PlayMode = when (current) {
    PlayMode.SEQUENCE -> PlayMode.REPEAT_ONE
    PlayMode.REPEAT_ONE -> PlayMode.SHUFFLE
    PlayMode.SHUFFLE -> PlayMode.SEQUENCE
}

internal fun repeatModeFor(mode: PlayMode): Int = when (mode) {
    PlayMode.REPEAT_ONE -> Player.REPEAT_MODE_ONE
    PlayMode.SHUFFLE -> Player.REPEAT_MODE_ALL
    PlayMode.SEQUENCE -> Player.REPEAT_MODE_OFF
}

internal fun shuffleEnabledFor(mode: PlayMode): Boolean = mode == PlayMode.SHUFFLE
