package com.example.player

import android.content.Context
import android.media.audiofx.Equalizer
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.model.Song
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@OptIn(UnstableApi::class)
class MusicPlayerHandler(private val context: Context) {

    private val _exoPlayer: ExoPlayer = ExoPlayer.Builder(context.applicationContext).build().apply {
        val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(androidx.media3.common.C.USAGE_MEDIA)
            .build()
        setAudioAttributes(audioAttributes, true)
    }
    val exoPlayer: ExoPlayer get() = _exoPlayer

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _isRepeatEnabled = MutableStateFlow(false)
    val isRepeatEnabled: StateFlow<Boolean> = _isRepeatEnabled.asStateFlow()

    // Equalizer settings
    private var physicalEqualizer: Equalizer? = null
    
    private val _equalizerEnabled = MutableStateFlow(false)
    val equalizerEnabled: StateFlow<Boolean> = _equalizerEnabled.asStateFlow()

    private val _equalizerBands = MutableStateFlow(List(5) { 0 }) // in dB
    val equalizerBands: StateFlow<List<Int>> = _equalizerBands.asStateFlow()

    private var currentPlaylistQueue = listOf<Song>()
    private var currentQueueIndex = -1

    private var progressJob: Job? = null
    private val playerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        _exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    startProgressTracking()
                } else {
                    stopProgressTracking()
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    _duration.value = _exoPlayer.duration.coerceAtLeast(0L)
                    initializeEqualizerIfNeeded()
                } else if (state == Player.STATE_ENDED) {
                    _currentPosition.value = 0L
                    if (_isRepeatEnabled.value) {
                        _exoPlayer.seekTo(0)
                        _exoPlayer.play()
                    } else {
                        skipToNext()
                    }
                }
            }
        })
    }

    fun playSong(song: Song, queue: List<Song> = listOf(song)) {
        currentPlaylistQueue = queue
        currentQueueIndex = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        
        _currentSong.value = song
        _currentPosition.value = 0L

        val mediaItem = MediaItem.fromUri(song.uriString)
        _exoPlayer.setMediaItem(mediaItem)
        _exoPlayer.prepare()
        _exoPlayer.play()
    }

    fun togglePlayPause() {
        if (_exoPlayer.isPlaying) {
            _exoPlayer.pause()
        } else {
            if (_exoPlayer.playbackState == Player.STATE_ENDED) {
                _exoPlayer.seekTo(0)
            }
            _exoPlayer.play()
        }
    }

    fun skipToNext() {
        if (currentPlaylistQueue.isEmpty()) return
        
        if (_isShuffleEnabled.value) {
            currentQueueIndex = (currentPlaylistQueue.indices).random()
        } else {
            currentQueueIndex = (currentQueueIndex + 1) % currentPlaylistQueue.size
        }
        
        val nextSong = currentPlaylistQueue.getOrNull(currentQueueIndex)
        if (nextSong != null) {
            playSong(nextSong, currentPlaylistQueue)
        }
    }

    fun skipToPrevious() {
        if (currentPlaylistQueue.isEmpty()) return
        
        currentQueueIndex = if (currentQueueIndex - 1 < 0) {
            currentPlaylistQueue.size - 1
        } else {
            currentQueueIndex - 1
        }
        
        val prevSong = currentPlaylistQueue.getOrNull(currentQueueIndex)
        if (prevSong != null) {
            playSong(prevSong, currentPlaylistQueue)
        }
    }

    fun seekTo(positionMs: Long) {
        _exoPlayer.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    fun toggleShuffle() {
        _isShuffleEnabled.value = !_isShuffleEnabled.value
    }

    fun toggleRepeat() {
        _isRepeatEnabled.value = !_isRepeatEnabled.value
    }

    // Audio Equalizer Implementation
    private fun initializeEqualizerIfNeeded() {
        val audioSessionId = _exoPlayer.audioSessionId
        if (audioSessionId != androidx.media3.common.C.AUDIO_SESSION_ID_UNSET && physicalEqualizer == null) {
            try {
                physicalEqualizer = Equalizer(0, audioSessionId).apply {
                    enabled = _equalizerEnabled.value
                    // Standardize 5 bands if supported
                    if (numberOfBands >= 5) {
                        for (i in 0 until 5) {
                            val level = _equalizerBands.value[i]
                            val range = bandLevelRange
                            val minLevel = range[0]
                            val maxLevel = range[1]
                            // Map level from -15dB to +15dB or clamp
                            val levelMillibels = (level * 100).coerceIn(minLevel.toInt(), maxLevel.toInt()).toShort()
                            setBandLevel(i.toShort(), levelMillibels)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleEqualizer(enabled: Boolean) {
        _equalizerEnabled.value = enabled
        try {
            physicalEqualizer?.enabled = enabled
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateEqualizerBand(bandIndex: Int, value: Int) {
        val updated = _equalizerBands.value.toMutableList()
        if (bandIndex in updated.indices) {
            updated[bandIndex] = value
            _equalizerBands.value = updated
        }
        
        try {
            physicalEqualizer?.let { eq ->
                if (bandIndex < eq.numberOfBands) {
                    val range = eq.bandLevelRange
                    val minLevel = range[0]
                    val maxLevel = range[1]
                    val levelMillibels = (value * 100).coerceIn(minLevel.toInt(), maxLevel.toInt()).toShort()
                    eq.setBandLevel(bandIndex.toShort(), levelMillibels)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun applyPreset(presetIndex: Int) {
        // Equalizer presets map (Bass Boost, Vocal, Electronic, Rock, Default)
        val presets = listOf(
            listOf(5, 3, 1, 3, 5),    // Classical / Default Pop
            listOf(8, 6, 0, 0, -2),   // Bass Power
            listOf(-2, 0, 4, 6, 4),   // Vocal Focus
            listOf(6, 4, -1, 4, 6),   // Electronic Dance
            listOf(0, 0, 0, 0, 0)     // Flat / Normal
        )
        val selected = presets.getOrNull(presetIndex) ?: presets.last()
        _equalizerBands.value = selected
        
        for (i in selected.indices) {
            try {
                physicalEqualizer?.let { eq ->
                    if (i < eq.numberOfBands) {
                        val range = eq.bandLevelRange
                        val minLevel = range[0]
                        val maxLevel = range[1]
                        val levelMillibels = (selected[i] * 100).coerceIn(minLevel.toInt(), maxLevel.toInt()).toShort()
                        eq.setBandLevel(i.toShort(), levelMillibels)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = playerScope.launch {
            while (isActive) {
                _currentPosition.value = _exoPlayer.currentPosition
                delay(300)
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
    }

    fun release() {
        playerScope.cancel()
        _exoPlayer.release()
        try {
            physicalEqualizer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
