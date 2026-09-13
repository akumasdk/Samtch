package com.akumasdk.samtch.ui.screens.player.viewmodel

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PlayerViewModelTest {

    private lateinit var viewModel: PlayerViewModel

    @Before
    fun setUp() {
        viewModel = PlayerViewModel()
    }

    @Test
    fun getChatRatio_autoMode_xperia21by9_returnsExactFitRatio() {
        // Sony Xperia 1 V: 960dp x 411dp (21:9 aspect ratio = ~2.335)
        val landscapeWidth = 960.dp
        val landscapeHeight = 411.dp

        val ratio = viewModel.getChatRatio(
            chatRatioPercent = 0,
            screenWidth = landscapeWidth,
            screenHeight = landscapeHeight,
            isFullscreen = true
        )

        // idealPlayerWidth = 411 * 16 / 9 = 730.666dp
        // autoRatio = 1 - (730.666 / 960) = ~0.2388 (23.88%)
        val expectedRatio = 1f - ((411f * (16f / 9f)) / 960f)
        assertEquals(expectedRatio, ratio, 0.001f)

        // Verify video player aspect ratio with this chat ratio
        val playerWidth = 960f * (1f - ratio)
        val playerAspectRatio = playerWidth / 411f
        assertEquals(16f / 9f, playerAspectRatio, 0.01f)
    }

    @Test
    fun getChatRatio_autoMode_20by9Phone_returnsExactFitRatio() {
        // Standard 20:9 phone: 914dp x 411dp (~2.223 aspect ratio)
        val landscapeWidth = 914.dp
        val landscapeHeight = 411.dp

        val ratio = viewModel.getChatRatio(
            chatRatioPercent = 0,
            screenWidth = landscapeWidth,
            screenHeight = landscapeHeight,
            isFullscreen = true
        )

        val expectedRatio = 1f - ((411f * (16f / 9f)) / 914f)
        assertEquals(expectedRatio, ratio, 0.001f)

        val playerWidth = 914f * (1f - ratio)
        val playerAspectRatio = playerWidth / 411f
        assertEquals(16f / 9f, playerAspectRatio, 0.01f)
    }

    @Test
    fun getChatRatio_autoMode_foldableCoverScreen_returnsExactFitRatio() {
        // Ultra-wide foldable cover screen: 960dp x 374dp (~2.566 aspect ratio)
        val landscapeWidth = 960.dp
        val landscapeHeight = 374.dp

        val ratio = viewModel.getChatRatio(
            chatRatioPercent = 0,
            screenWidth = landscapeWidth,
            screenHeight = landscapeHeight,
            isFullscreen = true
        )

        val expectedRatio = 1f - ((374f * (16f / 9f)) / 960f)
        assertEquals(expectedRatio, ratio, 0.001f)

        val playerWidth = 960f * (1f - ratio)
        val playerAspectRatio = playerWidth / 374f
        assertEquals(16f / 9f, playerAspectRatio, 0.01f)
    }

    @Test
    fun getChatRatio_autoMode_18by9Phone_floorsAtMinReadableChatRatio() {
        // 18:9 phone: 822dp x 411dp (2.0 aspect ratio)
        val landscapeWidth = 822.dp
        val landscapeHeight = 411.dp

        val ratio = viewModel.getChatRatio(
            chatRatioPercent = 0,
            screenWidth = landscapeWidth,
            screenHeight = landscapeHeight,
            isFullscreen = true
        )

        // minReadableChatRatio = 180 / 822 = ~0.2189
        val minReadable = 180f / 822f
        assertEquals(minReadable, ratio, 0.001f)
        assertTrue("Chat width in dp must be at least 180dp", ratio * 822f >= 179.9f)
    }

    @Test
    fun getChatRatio_autoMode_16by9Phone_returnsDefaultRatio() {
        // Standard 16:9 screen: 640dp x 360dp
        val landscapeWidth = 640.dp
        val landscapeHeight = 360.dp

        val ratio = viewModel.getChatRatio(
            chatRatioPercent = 0,
            screenWidth = landscapeWidth,
            screenHeight = landscapeHeight,
            isFullscreen = true
        )

        assertEquals(0.28f, ratio, 0.001f)
    }

    @Test
    fun getChatRatio_autoMode_portraitDimensionsPassed_normalizesToLandscape() {
        // Passing portrait dimensions (411dp x 960dp) instead of landscape
        val portraitWidth = 411.dp
        val portraitHeight = 960.dp

        val ratio = viewModel.getChatRatio(
            chatRatioPercent = 0,
            screenWidth = portraitWidth,
            screenHeight = portraitHeight,
            isFullscreen = true
        )

        // Should produce the exact same ratio as 960dp x 411dp
        val expectedRatio = 1f - ((411f * (16f / 9f)) / 960f)
        assertEquals(expectedRatio, ratio, 0.001f)
    }

    @Test
    fun getChatRatio_manualMode_honorsUserPercentage() {
        val ratio = viewModel.getChatRatio(
            chatRatioPercent = 35,
            screenWidth = 960.dp,
            screenHeight = 411.dp,
            isFullscreen = true
        )

        assertEquals(0.35f, ratio, 0.001f)
    }

    @Test
    fun getChatRatio_temporarilyExpanded_coercesAtLeast32Percent() {
        viewModel.isChatTemporarilyExpanded = true

        val ratio = viewModel.getChatRatio(
            chatRatioPercent = 0,
            screenWidth = 960.dp,
            screenHeight = 411.dp,
            isFullscreen = true
        )

        assertTrue(ratio >= 0.32f)
    }
}
