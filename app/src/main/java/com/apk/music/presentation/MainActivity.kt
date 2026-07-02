package com.apk.music.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.BitmapFactoryDecoder
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.apk.music.presentation.theme.MusicTheme
import com.apk.music.presentation.viewmodel.MusicViewModel

class MainActivity : ComponentActivity(), ImageLoaderFactory {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MusicApp()
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(BitmapFactoryDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.10)
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .crossfade(false)
            .allowHardware(true)
            .build()
    }
}

@Composable
fun MusicApp() {
    val navController = rememberSwipeDismissableNavController()
    val musicViewModel: MusicViewModel = viewModel()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Lifecycle Observer for Battery Saving
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> musicViewModel.onForegroundChanged(true)
                Lifecycle.Event.ON_PAUSE -> musicViewModel.onForegroundChanged(false)
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val permission = if (Build.VERSION.SDK_INT >= 33) {
        "android.permission.READ_MEDIA_AUDIO"
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            musicViewModel.initController(context)
        }
    }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) {
            launcher.launch(permission)
        } else {
            musicViewModel.initController(context)
        }
    }

    MusicTheme {
        AppScaffold {
            SwipeDismissableNavHost(
                navController = navController,
                startDestination = "library"
            ) {
                composable("library") {
                    val tracks by musicViewModel.tracksState
                    val isScanning by musicViewModel.isScanningState
                    
                    LibraryScreen(
                        tracks = tracks,
                        isScanning = isScanning,
                        onTrackClick = { track ->
                            musicViewModel.playTrack(track)
                            navController.navigate("now_playing")
                        },
                        onRefreshClick = {
                            musicViewModel.refreshLibrary(context)
                        }
                    )
                }
                composable("now_playing") {
                    val title by musicViewModel.currentTrackTitle
                    val artist by musicViewModel.currentArtist
                    val artworkUri by musicViewModel.coverArtUri
                    val isPlaying by musicViewModel.isPlaying
                    val isShuffleEnabled by musicViewModel.isShuffleEnabled
                    
                    NowPlayingScreen(
                        title = title,
                        artist = artist,
                        artworkUri = artworkUri,
                        isPlaying = isPlaying,
                        isShuffleEnabled = isShuffleEnabled,
                        progressProvider = { musicViewModel.progress.value },
                        onPlayPauseClick = { musicViewModel.playPause() },
                        onNextClick = { musicViewModel.next() },
                        onPreviousClick = { musicViewModel.previous() },
                        onShuffleClick = { musicViewModel.toggleShuffle() },
                        onVolumeClick = { /* TODO */ },
                        onQueueClick = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
