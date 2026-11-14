package com.apk.music.data.sync

import android.os.Environment
import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.concurrent.CancellationException

class MusicReceiverService : WearableListenerService() {

    companion object {
        private const val TAG = "MusicReceiverService"
        const val PATH_PREFIX = "/music_transfer"

        // Application-wide static scope that survives service destroy/recreate cycles
        private val persistentJob = SupervisorJob()
        private val persistentScope = CoroutineScope(Dispatchers.IO + persistentJob)

        // Global mutex queue to enforce strict FIFO writing order across service restarts
        private val transferMutex = Mutex()
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
        Log.d(TAG, ">>> onDataChanged triggered! Events count: ${dataEvents.count} <<<")

        // 1. Synchronously freeze data items while the buffer is open
        // to prevent "Buffer is closed" crashes.
        val frozenItems = mutableListOf<DataItem>()
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val path = event.dataItem.uri.path ?: continue
                if (path.startsWith(PATH_PREFIX)) {
                    frozenItems.add(event.dataItem.freeze())
                }
            }
        }

        if (frozenItems.isEmpty()) return

        // 2. Dispatch frozen items into our persistent background queue
        persistentScope.launch {
            for (item in frozenItems) {
                transferMutex.withLock {
                    extractAndSaveAsset(item)
                }
            }
        }
    }

    private suspend fun extractAndSaveAsset(dataItem: DataItem) {
        try {
            val path = dataItem.uri.path ?: return
            val dataMapItem = DataMapItem.fromDataItem(dataItem)
            val dataMap = dataMapItem.dataMap

            val asset = dataMap.getAsset("audio_asset")
            val fileName = dataMap.getString("fileName") ?: "unknown_track.m4a"

            if (asset == null) {
                Log.w(TAG, "Failed: 'audio_asset' missing for path $path")
                return
            }

            Log.d(TAG, "Asset found for file: $fileName. Opening file descriptor...")

            val client = Wearable.getDataClient(applicationContext)
            val assetResponse = client.getFdForAsset(asset).await()
            val inputStream = assetResponse.inputStream

            if (inputStream == null) {
                Log.e(TAG, "Failed: Asset InputStream was null for $fileName")
                return
            }

            // POINT TO PUBLIC SHARED STORAGE: /Music/SyncedMusic
            val musicDir = File(Environment.getExternalStorageDirectory(), "Music/SyncedMusic")
            if (!musicDir.exists()) {
                musicDir.mkdirs()
            }

            val outputFile = File(musicDir, fileName)
            Log.d(TAG, "Writing asset bytes to public storage: ${outputFile.absolutePath}")

            inputStream.use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            Log.d(TAG, "SUCCESS: File fully saved -> ${outputFile.absolutePath} (${outputFile.length()} bytes)")

        } catch (e: Exception) {
            if (e is CancellationException) {
                Log.w(TAG, "Extraction cancelled for data item ${dataItem.uri}: ${e.message}")
            } else {
                Log.e(TAG, "Exception during asset extraction for ${dataItem.uri}: ${e.message}", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "MusicReceiverService instance unbound/destroyed.")
    }
}