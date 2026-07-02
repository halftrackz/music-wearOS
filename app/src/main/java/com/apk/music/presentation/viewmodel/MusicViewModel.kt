package com.apk.music.presentation.viewmodel

import android.content.ComponentName
import android.content.Context
import android.media.MediaScannerConnection
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.apk.music.data.repository.TrackRepository
import com.apk.music.domain.model.Track
import com.apk.music.playback.PlaybackService
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class MusicViewModel : ViewModel() {
    private var controller: MediaController? = null
    
    private val _tracks = mutableStateOf<List<Track>>(emptyList())
    val tracksState: State<List<Track>> = _tracks
    
    val currentTrackTitle = mutableStateOf("No Track")
    val currentArtist = mutableStateOf("Unknown Artist")
    val isPlaying = mutableStateOf(false)
    val isShuffleEnabled = mutableStateOf(false)
    val progress = mutableStateOf(0f)
    val coverArtUri = mutableStateOf<android.net.Uri?>(null)
    
    private val _isScanning = mutableStateOf(false)
    val isScanningState: State<Boolean> = _isScanning

    private var progressJob: Job? = null
    private var isAppInForeground = true

    fun initController(context: Context) {
        if (controller != null) return
        
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener({
            controller = controllerFuture.get()
            setupController()
            if (_tracks.value.isNotEmpty()) {
                updatePlaylist()
            }
        }, MoreExecutors.directExecutor())
        
        loadTracks(context)
    }

    fun onForegroundChanged(isForeground: Boolean) {
        isAppInForeground = isForeground
        if (isForeground) {
            startProgressUpdate()
        } else {
            stopProgressUpdate()
        }
    }

    fun refreshLibrary(context: Context) {
        if (_isScanning.value) return
        _isScanning.value = true
        
        viewModelScope.launch(Dispatchers.IO) {
            val musicDir = File("/sdcard/Music")
            if (musicDir.exists()) {
                val files = musicDir.listFiles()?.map { it.absolutePath }?.toTypedArray()
                if (!files.isNullOrEmpty()) {
                    MediaScannerConnection.scanFile(context, files, null) { _, _ -> }
                }
            }
            
            delay(1500)
            
            val repository = TrackRepository(context)
            val scannedTracks = repository.scanTracks()
            
            launch(Dispatchers.Main) {
                _tracks.value = scannedTracks
                updatePlaylist()
                _isScanning.value = false
            }
        }
    }

    private fun loadTracks(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val repository = TrackRepository(context)
            val scannedTracks = repository.scanTracks()
            launch(Dispatchers.Main) {
                _tracks.value = scannedTracks
                updatePlaylist()
            }
        }
    }

    private fun updatePlaylist() {
        val controller = controller ?: return
        val currentTracks = _tracks.value
        if (currentTracks.isEmpty()) return

        val mediaItems = currentTracks.map { track ->
            MediaItem.Builder()
                .setMediaId(track.id)
                .setUri(track.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setArtworkUri(track.artworkUri)
                        .build()
                )
                .build()
        }
        controller.setMediaItems(mediaItems)
        controller.prepare()
    }

    private fun setupController() {
        controller?.addListener(object : Player.Listener {
            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                currentTrackTitle.value = mediaMetadata.title?.toString() ?: "No Track"
                currentArtist.value = mediaMetadata.artist?.toString() ?: "Unknown Artist"
                coverArtUri.value = mediaMetadata.artworkUri
            }

            override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                isPlaying.value = isPlayingNow
                if (isPlayingNow) startProgressUpdate() else stopProgressUpdate()
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                isShuffleEnabled.value = shuffleModeEnabled
            }
        })
        isShuffleEnabled.value = controller?.shuffleModeEnabled ?: false
        isPlaying.value = controller?.isPlaying ?: false
        if (isPlaying.value) startProgressUpdate()
    }

    private fun startProgressUpdate() {
        if (!isAppInForeground || !isPlaying.value) return
        
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                controller?.let {
                    if (it.isPlaying && it.duration > 0) {
                        progress.value = it.currentPosition.toFloat() / it.duration.toFloat()
                    }
                }
                // Update every 2 seconds instead of 1 to save battery
                delay(2000)
            }
        }
    }

    private fun stopProgressUpdate() {
        progressJob?.cancel()
        progressJob = null
    }

    fun playTrack(track: Track) {
        val controller = controller ?: return
        val index = _tracks.value.indexOf(track)
        if (index != -1) {
            controller.seekTo(index, 0)
            controller.play()
        }
    }

    fun playPause() {
        if (controller?.isPlaying == true) {
            controller?.pause()
        } else {
            controller?.play()
        }
    }

    fun toggleShuffle() {
        controller?.shuffleModeEnabled = !isShuffleEnabled.value
    }

    fun next() {
        controller?.seekToNext()
    }

    fun previous() {
        controller?.seekToPrevious()
    }

    override fun onCleared() {
        stopProgressUpdate()
        controller?.release()
        super.onCleared()
    }
}
