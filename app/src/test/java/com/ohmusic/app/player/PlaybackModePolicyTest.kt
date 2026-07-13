package com.ohmusic.app.player

import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackModePolicyTest {

    @Test
    fun `mode cycles through sequence repeat one and shuffle`() {
        assertEquals(PlayMode.REPEAT_ONE, nextPlayMode(PlayMode.SEQUENCE))
        assertEquals(PlayMode.SHUFFLE, nextPlayMode(PlayMode.REPEAT_ONE))
        assertEquals(PlayMode.SEQUENCE, nextPlayMode(PlayMode.SHUFFLE))
    }

    @Test
    fun `mode maps to Media3 repeat and shuffle flags`() {
        assertEquals(Player.REPEAT_MODE_OFF, repeatModeFor(PlayMode.SEQUENCE))
        assertEquals(Player.REPEAT_MODE_ONE, repeatModeFor(PlayMode.REPEAT_ONE))
        assertEquals(Player.REPEAT_MODE_ALL, repeatModeFor(PlayMode.SHUFFLE))
        assertTrue(shuffleEnabledFor(PlayMode.SHUFFLE))
        assertFalse(shuffleEnabledFor(PlayMode.REPEAT_ONE))
    }
}
