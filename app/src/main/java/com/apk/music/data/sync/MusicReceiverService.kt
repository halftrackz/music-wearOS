package com.apk.music.data.sync

import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.apk.music.data.repository.TrackRepository
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class MusicReceiverService : WearableListenerService() {
    
    companion object {
        private const val TAG = "MusicReceiverService"
        private const val MUSIC_PATH_PREFIX = "/music_transfer/"
        const val ACTION_TRACK_RECEIVED = "com.apk.music.ACTION_TRACK_RECEIVED"
        private const val PATH_REQUEST_TRACK_LIST = "/request_track_list"
        private const val PATH_TRACK_LIST_RESPONSE = "/track_list_response"
        private const val PATH_DELETE_TRACK = "/delete_track"
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
                MediaScannerConnection.scanFile(this, arrayOf(targetFile.absolutePath), null) { _, _ ->
                    val intent = Intent(ACTION_TRACK_RECEIVED).setPackage(packageName)
                    sendBroadcast(intent)
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            PATH_REQUEST_TRACK_LIST -> {
                val repository = TrackRepository(this)
                val tracks = repository.scanTracks()
                val jsonArray = JSONArray()
                tracks.forEach { track ->
                    val obj = JSONObject().apply {
                        put("id", track.id)
                        put("title", track.title)
                        put("artist", track.artist)
                        put("duration", track.duration)
                    }
                    jsonArray.put(obj)
                }
                val payload = jsonArray.toString().toByteArray(Charsets.UTF_8)
                Wearable.getMessageClient(this).sendMessage(
                    messageEvent.sourceNodeId,
                    PATH_TRACK_LIST_RESPONSE,
                    payload
                )
            }
            PATH_DELETE_TRACK -> {
                val fileName = String(messageEvent.data, Charsets.UTF_8)
                val musicDir = File(Environment.getExternalStorageDirectory(), "Music")
                val targetFile = File(musicDir, fileName)
                if (targetFile.exists()) {
                    targetFile.delete()
                    MediaScannerConnection.scanFile(this, arrayOf(targetFile.absolutePath), null) { _, _ ->
                        val intent = Intent(ACTION_TRACK_RECEIVED).setPackage(packageName)
                        sendBroadcast(intent)
                    }
                }
            }
        }
    }
}
