package com.example.ui

import android.app.Application
import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.MusicRepository
import com.example.data.Song
import com.example.data.database.AppDatabase
import com.example.data.database.PlaylistEntity
import com.example.data.database.PlaylistSongEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class MusicViewModel(
    application: Application,
    private val repository: MusicRepository
) : AndroidViewModel(application) {

    private val TAG = "MusicViewModel"
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    // Real-time states
    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())
    val allSongs: StateFlow<List<Song>> = _allSongs.asStateFlow()

    private val _playlists = MutableStateFlow<List<PlaylistEntity>>(emptyList())
    val playlists: StateFlow<List<PlaylistEntity>> = _playlists.asStateFlow()

    // Current playlist being viewed (null if viewing All Songs)
    private val _selectedPlaylist = MutableStateFlow<PlaylistEntity?>(null)
    val selectedPlaylist: StateFlow<PlaylistEntity?> = _selectedPlaylist.asStateFlow()

    private val _playlistSongs = MutableStateFlow<List<Song>>(emptyList())
    val playlistSongs: StateFlow<List<Song>> = _playlistSongs.asStateFlow()

    // Media Player states
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    // Permissions and files configuration
    private val _permissionGranted = MutableStateFlow(false)
    val permissionGranted: StateFlow<Boolean> = _permissionGranted.asStateFlow()

    // Current playing active queue (All Songs, or a specific Playlist)
    private val _currentQueue = MutableStateFlow<List<Song>>(emptyList())
    val currentQueue: StateFlow<List<Song>> = _currentQueue.asStateFlow()

    init {
        // Collect database playlists
        viewModelScope.launch {
            repository.allPlaylists.collect { list ->
                _playlists.value = list
            }
        }

        // Initialize and generate default demo/offline tracks so the app is immediately usable
        viewModelScope.launch {
            repository.generateDemoTracksIfNeeded()
            refreshSongs()
        }
    }

    fun setPermissionGranted(granted: Boolean) {
        _permissionGranted.value = granted
        refreshSongs()
    }

    fun refreshSongs() {
        viewModelScope.launch {
            repository.scanDeviceSongs(_permissionGranted.value).collect { list ->
                _allSongs.value = list
                // If queue is empty, default to all detected/generated songs
                if (_currentQueue.value.isEmpty()) {
                    _currentQueue.value = list
                }
            }
        }
    }

    // Playback control flow
    fun selectAndPlay(song: Song, queue: List<Song>) {
        _currentQueue.value = queue
        _currentSong.value = song
        playSongFile(song)
    }

    private fun playSongFile(song: Song) {
        try {
            stopProgressPolling()
            mediaPlayer?.release()
            mediaPlayer = null

            mediaPlayer = MediaPlayer().apply {
                // If it is a demo track or external storage file, use the absolute path directly
                setDataSource(song.path)
                prepare()
                start()
                
                _isPlaying.value = true
                _duration.value = duration.toLong()
                _currentPosition.value = 0L

                setOnCompletionListener {
                    _isPlaying.value = false
                    _currentPosition.value = duration.toLong()
                    playNext()
                }
            }

            startProgressPolling()
        } catch (e: Exception) {
            Log.e(TAG, "Error playing song: ${e.message}", e)
            _isPlaying.value = false
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            _isPlaying.value = false
            stopProgressPolling()
        } else {
            player.start()
            _isPlaying.value = true
            startProgressPolling()
        }
    }

    fun playNext() {
        val queue = _currentQueue.value
        val song = _currentSong.value ?: return
        if (queue.isEmpty()) return

        val currentIndex = queue.indexOfFirst { it.path == song.path }
        if (currentIndex != -1) {
            val nextIndex = (currentIndex + 1) % queue.size
            val nextSong = queue[nextIndex]
            _currentSong.value = nextSong
            playSongFile(nextSong)
        }
    }

    fun playPrevious() {
        val queue = _currentQueue.value
        val song = _currentSong.value ?: return
        if (queue.isEmpty()) return

        val currentIndex = queue.indexOfFirst { it.path == song.path }
        if (currentIndex != -1) {
            val prevIndex = if (currentIndex - 1 < 0) queue.size - 1 else currentIndex - 1
            val prevSong = queue[prevIndex]
            _currentSong.value = prevSong
            playSongFile(prevSong)
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let { player ->
            player.seekTo(positionMs.toInt())
            _currentPosition.value = positionMs
        }
    }

    private fun startProgressPolling() {
        progressJob = viewModelScope.launch {
            while (true) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        _currentPosition.value = player.currentPosition.toLong()
                    }
                }
                delay(250)
            }
        }
    }

    private fun stopProgressPolling() {
        progressJob?.cancel()
        progressJob = null
    }

    // Playlist Controls
    fun selectPlaylist(playlist: PlaylistEntity?) {
        _selectedPlaylist.value = playlist
        if (playlist == null) {
            _playlistSongs.value = emptyList()
        } else {
            viewModelScope.launch {
                repository.getSongsForPlaylist(playlist.id).collect { list ->
                    // Map PlaylistSongEntity back to the common UI Song structure
                    _playlistSongs.value = list.map { entity ->
                        Song(
                            id = entity.id.toString(),
                            title = entity.title,
                            artist = entity.artist,
                            album = entity.album,
                            path = entity.path,
                            duration = entity.duration,
                            isDemo = entity.path.contains("demo_songs"),
                            year = entity.year
                        )
                    }
                }
            }
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            val id = repository.createPlaylist(name)
            Log.d(TAG, "Created playlist with ID: $id")
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch {
            if (_selectedPlaylist.value?.id == id) {
                _selectedPlaylist.value = null
            }
            repository.deletePlaylist(id)
        }
    }

    fun toggleSongInPlaylist(playlistId: Long, song: Song, inPlaylist: Boolean) {
        viewModelScope.launch {
            if (inPlaylist) {
                repository.removeSongFromPlaylist(playlistId, song.path)
            } else {
                repository.addSongToPlaylist(playlistId, song)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopProgressPolling()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    /**
     * Boilerplate Factory for ViewModel creation
     */
    class Factory(
        private val application: Application,
        private val repository: MusicRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MusicViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MusicViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
