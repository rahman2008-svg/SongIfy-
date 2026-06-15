package com.example.data.repository

import android.content.Context
import android.provider.MediaStore
import com.example.data.database.PlaylistDao
import com.example.data.database.SongDao
import com.example.data.model.Playlist
import com.example.data.model.PlaylistSongCrossRef
import com.example.data.model.Song
import kotlinx.coroutines.flow.Flow

class MusicRepository(
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao
) {
    val allSongs: Flow<List<Song>> = songDao.getAllSongs()
    val favoriteSongs: Flow<List<Song>> = songDao.getFavoriteSongs()
    val allPlaylists: Flow<List<Playlist>> = playlistDao.getAllPlaylists()

    suspend fun toggleFavorite(songId: Long, isFavorite: Boolean) {
        songDao.updateFavoriteStatus(songId, isFavorite)
    }

    suspend fun getSongById(songId: Long): Song? {
        return songDao.getSongById(songId)
    }

    suspend fun createPlaylist(name: String): Long {
        return playlistDao.insertPlaylist(Playlist(name = name))
    }

    suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deletePlaylist(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        playlistDao.addSongToPlaylist(PlaylistSongCrossRef(playlistId, songId))
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        playlistDao.removeSongFromPlaylist(playlistId, songId)
    }

    fun getSongsInPlaylist(playlistId: Long): Flow<List<Song>> {
        return playlistDao.getSongsInPlaylist(playlistId)
    }

    suspend fun scanLocalMusic(context: Context): Int {
        val songList = mutableListOf<Song>()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        try {
            context.contentResolver.query(uri, projection, selection, null, null)?.use { cursor ->
                if (cursor.count > 0) {
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                    val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val title = cursor.getString(titleCol) ?: "Unknown Song"
                        val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                        val album = cursor.getString(albumCol) ?: "Unknown Album"
                        val duration = cursor.getLong(durationCol)
                        val path = cursor.getString(dataCol) ?: ""
                        val uriString = "${MediaStore.Audio.Media.EXTERNAL_CONTENT_URI}/$id"

                        songList.add(
                            Song(
                                id = id,
                                title = title,
                                artist = artist,
                                album = album,
                                duration = duration,
                                path = path,
                                uriString = uriString,
                                isFavorite = false,
                                isDemo = false
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (songList.isNotEmpty()) {
            songDao.deleteScannedSongs()
            songDao.insertSongs(songList)
        }
        return songList.size
    }

    suspend fun insertDemoSongs() {
        val demos = listOf(
            Song(
                id = 990001L,
                title = "Synthwave Chill",
                artist = "Prince AR Abdur Rahman",
                album = "NexVora Digital Beats",
                duration = 372000L,
                path = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                isFavorite = false,
                isDemo = true
            ),
            Song(
                id = 990002L,
                title = "Acoustic Sunset",
                artist = "Prince AR Abdur Rahman",
                album = "Warm Horizon Ambient",
                duration = 302000L,
                path = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                isFavorite = true,
                isDemo = true
            ),
            Song(
                id = 990003L,
                title = "LoFi Lounge Beats",
                artist = "Prince AR Abdur Rahman",
                album = "Midnight Coffee Study",
                duration = 318000L,
                path = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
                uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
                isFavorite = false,
                isDemo = true
            ),
            Song(
                id = 990004L,
                title = "Techno Vibes Pro",
                artist = "Prince AR Abdur Rahman",
                album = "NexPlay Pulse",
                duration = 445000L,
                path = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3",
                uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3",
                isFavorite = false,
                isDemo = true
            )
        )
        songDao.insertSongs(demos)
    }
}
