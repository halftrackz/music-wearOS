package com.apk.music.data.sync

import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.WearableListenerService
import java.io.File
import java.io.FileOutputStream

class MusicSyncService : WearableListenerService() {

    override fun onChannelOpened(channel: ChannelClient.Channel) {
        // Here we would handle the file transfer via ChannelClient
        // For simplicity in this clone, we'll assume the phone sends the file content
        val musicDir = File(filesDir, "music")
        if (!musicDir.exists()) musicDir.mkdirs()

        val fileName = channel.path.substringAfterLast("/")
        val targetFile = File(musicDir, fileName)

        val channelClient = com.google.android.gms.wearable.Wearable.getChannelClient(this)
        channelClient.receiveFile(channel, android.net.Uri.fromFile(targetFile), false)
    }
}
