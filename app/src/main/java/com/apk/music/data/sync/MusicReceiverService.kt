package com.apk.music.data.sync

import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import java.io.File

class MusicReceiverService : WearableListenerService() {
    
    companion object {
        private const val TAG = "MusicReceiverService"
        private const val MUSIC_PATH_PREFIX = "/music_transfer/"
    }

    override fun onChannelOpened(channel: ChannelClient.Channel) {
        val path = channel.path
        if (path.startsWith(MUSIC_PATH_PREFIX)) {
            val fileName = path.substring(MUSIC_PATH_PREFIX.length)
            if (fileName.isEmpty()) return

            val musicDir = File(Environment.getExternalStorageDirectory(), "Music")
            if (!musicDir.exists()) musicDir.mkdirs()

            val targetFile = File(musicDir, fileName)
            
            Log.d(TAG, "Receiving file: $fileName into ${targetFile.absolutePath}")

            val channelClient = Wearable.getChannelClient(this)
            
            // receiveFile is asynchronous
            channelClient.receiveFile(channel, Uri.fromFile(targetFile), false)
                .addOnSuccessListener {
                    Log.d(TAG, "File transfer started: $fileName")
                }
        }
    }

    override fun onInputClosed(channel: ChannelClient.Channel, closeReason: Int, appSpecificErrorCode: Int) {
        if (channel.path.startsWith(MUSIC_PATH_PREFIX) && closeReason == ChannelClient.ChannelCallback.CLOSE_REASON_NORMAL) {
            val fileName = channel.path.substring(MUSIC_PATH_PREFIX.length)
            val musicDir = File(Environment.getExternalStorageDirectory(), "Music")
            val targetFile = File(musicDir, fileName)
            
            if (targetFile.exists()) {
                Log.d(TAG, "File transfer complete. Notifying MediaScanner: ${targetFile.name}")
                // Automatically tell the watch system that a new song has arrived
                MediaScannerConnection.scanFile(this, arrayOf(targetFile.absolutePath), null) { _, _ -> }
            }
        }
    }
}
