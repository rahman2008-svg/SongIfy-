package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.MusicDatabase
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.data.repository.MusicRepository
import com.example.player.PlayerProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val database = MusicDatabase.getDatabase(application)
    private val repository = MusicRepository(database.songDao(), database.playlistDao())
    val playerHandler = PlayerProvider.getPlayerHandler(application)

    // UI state flows
    val allSongs: StateFlow<List<Song>> = repository.allSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteSongs: StateFlow<List<Song>> = repository.favoriteSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<Playlist>> = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Player States derived from PlayerHandler
    val currentSong: StateFlow<Song?> = playerHandler.currentSong
    val isPlaying: StateFlow<Boolean> = playerHandler.isPlaying
    val currentPosition: StateFlow<Long> = playerHandler.currentPosition
    val duration: StateFlow<Long> = playerHandler.duration
    val isShuffleEnabled: StateFlow<Boolean> = playerHandler.isShuffleEnabled
    val isRepeatEnabled: StateFlow<Boolean> = playerHandler.isRepeatEnabled

    // Equalizer States
    val equalizerEnabled: StateFlow<Boolean> = playerHandler.equalizerEnabled
    val equalizerBands: StateFlow<List<Int>> = playerHandler.equalizerBands

    // Other UI interactive states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val _scanResultCount = MutableStateFlow<Int?>(null)
    val scanResultCount = _scanResultCount.asStateFlow()

    private val _currentSelectedPlaylist = MutableStateFlow<Playlist?>(null)
    val currentSelectedPlaylist = _currentSelectedPlaylist.asStateFlow()

    val currentPlaylistSongs: StateFlow<List<Song>> = _currentSelectedPlaylist
        .flatMapLatest { playlist ->
            if (playlist != null) {
                repository.getSongsInPlaylist(playlist.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredSongs: StateFlow<List<Song>> = combine(allSongs, searchQuery) { songs, query ->
        if (query.isBlank()) {
            songs
        } else {
            songs.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true) ||
                it.album.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Pre-populate with beautiful demo tracks on first run so the app works instantly
        viewModelScope.launch {
            allSongs.collectLatest { currentSongs ->
                if (currentSongs.isEmpty()) {
                    repository.insertDemoSongs()
                }
            }
        }
    }

    fun searchSongs(query: String) {
        _searchQuery.value = query
    }

    fun selectPlaylist(playlist: Playlist?) {
        _currentSelectedPlaylist.value = playlist
    }

    fun scanMusic(context: Context) {
        viewModelScope.launch {
            _isScanning.value = true
            _scanResultCount.value = null
            val count = repository.scanLocalMusic(context)
            _scanResultCount.value = count
            _isScanning.value = false
        }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            repository.toggleFavorite(song.id, !song.isFavorite)
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repository.createPlaylist(name)
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            repository.deletePlaylist(playlist.id)
            if (_currentSelectedPlaylist.value?.id == playlist.id) {
                _currentSelectedPlaylist.value = null
            }
        }
    }

    fun addSongToPlaylist(playlistId: Long, song: Song) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, song.id)
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, song: Song) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, song.id)
        }
    }

    fun playSong(song: Song, queue: List<Song> = emptyList()) {
        val actualQueue = if (queue.isEmpty()) filteredSongs.value else queue
        playerHandler.playSong(song, actualQueue)
    }

    fun togglePlayPause() {
        playerHandler.togglePlayPause()
    }
}
