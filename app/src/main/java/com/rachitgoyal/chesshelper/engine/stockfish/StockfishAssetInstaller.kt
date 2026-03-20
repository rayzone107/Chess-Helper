package com.rachitgoyal.chesshelper.engine.stockfish

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Locates the Stockfish binary that Android extracts from jniLibs at install time.
 *
 * The binary is packaged as `jniLibs/arm64-v8a/libstockfish.so` so that Android's
 * package installer extracts it to [applicationInfo.nativeLibraryDir] automatically.
 * This avoids all AssetManager / AAPT / GZIP issues that arise when bundling large
 * executables as assets.
 *
 * No runtime copying or decompression is needed — the file is already on the device
 * filesystem. We just set the executable bit and return the path.
 */
internal object StockfishAssetInstaller {
    private const val TAG = "StockfishInstaller"
    private const val NATIVE_LIB_NAME = "libstockfish.so"

    fun ensureInstalled(context: Context): File {
        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        Log.d(TAG, "nativeLibraryDir = $nativeLibDir")

        val binary = File(nativeLibDir, NATIVE_LIB_NAME)
        Log.d(TAG, "Stockfish binary path: ${binary.absolutePath} exists=${binary.exists()} size=${if (binary.exists()) binary.length() else -1}")

        if (!binary.exists()) {
            // List directory contents to aid diagnosis.
            val contents = File(nativeLibDir).listFiles()?.map { it.name } ?: emptyList<String>()
            Log.e(TAG, "libstockfish.so not found in nativeLibraryDir. Contents: $contents")
            throw IllegalStateException(
                "Stockfish binary not found at ${binary.absolutePath}. " +
                    "nativeLibraryDir contents: $contents. " +
                    "This means the APK was installed without the jniLibs. Try a clean reinstall."
            )
        }

        if (binary.length() == 0L) {
            throw IllegalStateException("Stockfish binary at ${binary.absolutePath} is 0 bytes — corrupt install.")
        }

        // The binary lives in nativeLibraryDir which is on an executable filesystem.
        // setExecutable is a no-op on most devices but harmless to call.
        val executable = binary.setExecutable(true, false)
        Log.d(TAG, "Stockfish ready: ${binary.absolutePath} (${binary.length()} bytes, setExecutable=$executable)")

        return binary
    }
}
