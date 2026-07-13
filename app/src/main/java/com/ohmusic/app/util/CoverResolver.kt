package com.ohmusic.app.util

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import java.io.File

/**
 * Resolves artwork from a previous local cache when available, then falls back
 * to the device MediaStore. The bitmap loader supplies the bundled placeholder
 * if MediaStore does not contain artwork for the album.
 */
object CoverResolver {
    private var coverDir: File? = null
    private var externalAppCoverDir: File? = null

    fun init(context: Context) {
        coverDir = File(context.filesDir, "covers").also { if (!it.exists()) it.mkdirs() }
        externalAppCoverDir = context.getExternalFilesDir(null)?.let { File(it, "covers") }
            ?.also { if (!it.exists()) it.mkdirs() }
    }

    fun resolve(albumId: Long): Uri {
        val internalFile = coverDir?.let { File(it, "$albumId.jpg") }
        if (internalFile != null && internalFile.exists() && internalFile.length() > 1000) {
            return Uri.fromFile(internalFile)
        }

        val externalFile = externalAppCoverDir?.let { File(it, "$albumId.jpg") }
        if (externalFile != null && externalFile.exists() && externalFile.length() > 1000) {
            return Uri.fromFile(externalFile)
        }

        return "content://media/external/audio/albumart/$albumId".toUri()
    }
}
