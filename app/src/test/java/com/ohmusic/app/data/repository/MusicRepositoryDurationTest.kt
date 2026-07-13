package com.ohmusic.app.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicRepositoryDurationTest {
    @Test
    fun `only songs longer than two minutes are eligible`() {
        assertFalse(isEligibleSongDuration(119_999L))
        assertFalse(isEligibleSongDuration(120_000L))
        assertTrue(isEligibleSongDuration(120_001L))
    }
}
