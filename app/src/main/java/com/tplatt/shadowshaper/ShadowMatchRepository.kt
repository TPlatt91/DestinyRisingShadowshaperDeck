package com.tplatt.shadowshaper

import android.content.Context
import android.util.AtomicFile
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.File

/**
 * Private, versioned checkpoint. AtomicFile prevents partially written saves
 * when Android kills the process. Never overwrites the user's built decks.
 */
internal class ShadowMatchRepository(context: Context) {
    private val file = AtomicFile(
        File(context.applicationContext.filesDir, "shadowshaper_active_match_v1.bin")
    )

    @Synchronized
    fun load(): ShadowSavedMatch? = try {
        ObjectInputStream(file.openRead()).use {
            val restored = it.readObject() as? ShadowSavedMatch
            restored?.takeIf { save ->
                save.format == ShadowMatchState.VERSION &&
                    save.engine.format == ShadowMatchState.VERSION &&
                    !save.engine.finished
            }
        }
    } catch (_: Exception) {
        null
    }

    @Synchronized
    fun save(match: ShadowSavedMatch): Boolean {
        if (match.engine.finished) {
            clear()
            return true
        }
        var stream: java.io.FileOutputStream? = null
        return try {
            stream = file.startWrite()
            ObjectOutputStream(stream).use { output ->
                output.writeObject(match)
                output.flush()
                // ObjectOutputStream.close() closes the underlying fd;
                // AtomicFile.finishWrite handles synchronization and commit.
            }
            file.finishWrite(stream)
            true
        } catch (_: Exception) {
            if (stream != null) file.failWrite(stream)
            false
        }
    }

    @Synchronized
    fun clear() { file.delete() }
}
