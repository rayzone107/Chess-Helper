package com.rachitgoyal.chesshelper.engine.stockfish

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StockfishAssetInstallerTest {

    @Test
    fun selectBinarySpecPrefersArm64WhenAvailable() {
        val spec = StockfishAssetInstaller.selectBinarySpec(listOf("arm64-v8a", "x86_64"))

        assertEquals("arm64-v8a", spec.abi)
        assertEquals("stockfish/stockfish-android-armv8", spec.assetPath)
        assertEquals("stockfish-android-armv8", spec.installedBinaryName)
    }

    @Test
    fun selectBinarySpecSupportsX8664Emulators() {
        val spec = StockfishAssetInstaller.selectBinarySpec(listOf("x86_64"))

        assertEquals("x86_64", spec.abi)
        assertEquals("stockfish/stockfish-android-x86_64", spec.assetPath)
        assertEquals("stockfish-android-x86_64", spec.installedBinaryName)
    }

    @Test
    fun selectBinarySpecFailsClearlyForUnsupportedAbi() {
        val error = runCatching {
            StockfishAssetInstaller.selectBinarySpec(listOf("armeabi-v7a"))
        }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertTrue(error?.message?.contains("Device ABIs: armeabi-v7a") == true)
    }
}

