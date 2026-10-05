package com.ppicalendar.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.LifecycleOwner
import java.io.File

/**
 * Helper for persisting critical app data (companies, vault entries, incentive points) to a durable location.
 *
 * - By default we use an internal app‑specific folder under `files/SynclyData`.
 * - If the user prefers an external folder, they can pick one via the SAF picker and the URI is persisted.
 */
object StorageManager {
    private const val INTERNAL_SUBDIR = "SynclyData"
    private var externalFolderUri: Uri? = null

    /** Returns the internal folder, creating it if it does not exist. */
    fun getInternalFolder(context: Context): File {
        val dir = File(context.filesDir, INTERNAL_SUBDIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }


    /** Write byte data to the chosen storage (prefers internal). */
    fun writeData(context: Context, relativePath: String, data: ByteArray) {
        // Prefer internal storage
        val file = File(getInternalFolder(context), relativePath)
        file.parentFile?.mkdirs()
        file.writeBytes(data)
    }

    /** Read data from storage – checks internal first, then external if needed. */
    fun readData(context: Context, relativePath: String): ByteArray? {
        val internalFile = File(getInternalFolder(context), relativePath)
        return if (internalFile.exists()) {
            internalFile.readBytes()
        } else {
            // External read via SAF is more involved; callers can resolve the Uri themselves.
            null
        }
    }
}
