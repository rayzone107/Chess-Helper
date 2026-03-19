package com.rachitgoyal.chesshelper.engine.stockfish

import android.content.Context
import java.io.File
import java.util.zip.GZIPInputStream

internal object StockfishAssetInstaller {
    private const val ASSET_PATH = "stockfish/stockfish-android-armv8.gz"
    private const val INSTALL_DIR = "stockfish"
    private const val INSTALLED_BINARY_NAME = "stockfish-android-armv8"

    fun ensureInstalled(context: Context): File {
        val installDir = File(context.noBackupFilesDir, INSTALL_DIR).apply { mkdirs() }
        val installedBinary = File(installDir, INSTALLED_BINARY_NAME)

        if (!installedBinary.exists() || installedBinary.length() == 0L) {
            context.assets.open(ASSET_PATH).use { compressedInput ->
                GZIPInputStream(compressedInput).use { input ->
                    installedBinary.outputStream().use { output -> input.copyTo(output) }
                }
            }
        }

        installedBinary.setReadable(true, true)
        installedBinary.setWritable(true, true)
        installedBinary.setExecutable(true, true)

        return installedBinary
    }
}

