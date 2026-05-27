package com.example.data

import android.content.ContentResolver
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import com.example.data.database.PlaylistDao
import com.example.data.database.PlaylistEntity
import com.example.data.database.PlaylistSongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

class MusicRepository(
    private val context: Context,
    private val playlistDao: PlaylistDao
) {
    private val TAG = "MusicRepository"

    val allPlaylists: Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()

    fun getSongsForPlaylist(playlistId: Long): Flow<List<PlaylistSongEntity>> {
        return playlistDao.getSongsForPlaylist(playlistId)
    }

    suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        playlistDao.insertPlaylist(PlaylistEntity(name = name))
    }

    suspend fun deletePlaylist(id: Long) = withContext(Dispatchers.IO) {
        playlistDao.deletePlaylistById(id)
    }

    suspend fun addSongToPlaylist(playlistId: Long, song: Song) = withContext(Dispatchers.IO) {
        playlistDao.insertPlaylistSong(
            PlaylistSongEntity(
                playlistId = playlistId,
                title = song.title,
                artist = song.artist,
                album = song.album,
                path = song.path,
                duration = song.duration,
                year = song.year
            )
        )
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, path: String) = withContext(Dispatchers.IO) {
        playlistDao.removeSongFromPlaylist(playlistId, path)
    }

    /**
     * Scans both:
     * 1. App-specific generated demo folder (no permission required).
     * 2. Device MediaStore audio records (permission requested from UI).
     */
    fun scanDeviceSongs(permissionGranted: Boolean): Flow<List<Song>> = flow {
        val songList = mutableListOf<Song>()

        // 1. Scan app-specific generated demo songs (WAV files inside filesDir/demo_songs)
        val demoDir = File(context.filesDir, "demo_songs")
        if (demoDir.exists() && demoDir.isDirectory) {
            val demoFiles = demoDir.listFiles { _, name -> name.endsWith(".wav") }
            demoFiles?.forEach { file ->
                val title = file.nameWithoutExtension.replace('_', ' ')
                val year = when {
                    file.name.contains("Brisa") -> "2026"
                    file.name.contains("Eco") -> "2025"
                    file.name.contains("Pulso") -> "2026"
                    file.name.contains("Melodia") -> "2024"
                    else -> "2026"
                }
                songList.add(
                    Song(
                        id = file.absolutePath,
                        title = title,
                        artist = "Sintetizador Offline",
                        album = "Sons de Bolso",
                        path = file.absolutePath,
                        duration = getWavDurationMs(file),
                        isDemo = true,
                        year = year
                    )
                )
            }
        }

        // 2. Scan standard MediaStore (if permission granted)
        if (permissionGranted) {
            try {
                val contentResolver: ContentResolver = context.contentResolver
                val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                val projection = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.YEAR
                )
                // Filter only music files or tracks with duration > 1000
                val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 1000"
                val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

                contentResolver.query(uri, projection, selection, null, sortOrder)?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                    val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                    val yearCol = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)

                    while (cursor.moveToNext()) {
                        val path = cursor.getString(dataCol)
                        // Make sure we don't add duplicate entries if it's somehow matching our demo files path
                        if (songList.none { it.path == path }) {
                            val mediaYear = if (yearCol != -1) cursor.getString(yearCol) else null
                            songList.add(
                                Song(
                                    id = cursor.getLong(idCol).toString(),
                                    title = cursor.getString(titleCol) ?: "Sem Título",
                                    artist = cursor.getString(artistCol) ?: "Artista Desconhecido",
                                    album = cursor.getString(albumCol) ?: "Álbum Desconhecido",
                                    path = path ?: "",
                                    duration = cursor.getLong(durationCol),
                                    isDemo = false,
                                    year = mediaYear
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error scanning MediaStore: ${e.message}", e)
            }
        }

        emit(songList)
    }.flowOn(Dispatchers.IO)

    /**
     * Generates standard high-quality ambient synth WAV files inside internal files directory
     * so that the application has real music playable offline out-of-the-box!
     */
    suspend fun generateDemoTracksIfNeeded(): Boolean = withContext(Dispatchers.IO) {
        val demoDir = File(context.filesDir, "demo_songs")
        if (!demoDir.exists()) {
            demoDir.mkdirs()
        }

        val demoFilesCount = demoDir.listFiles { _, name -> name.endsWith(".wav") }?.size ?: 0
        if (demoFilesCount >= 4) {
            return@withContext false // Already generated
        }

        try {
            // Write 4 different cozy ambient synthesized wave files
            createSynthWav(
                file = File(demoDir, "Brisa_Cosmica.wav"),
                frequency1 = 440.0, // A4
                frequency2 = 554.37, // C#5
                durationSeconds = 15.0
            )
            createSynthWav(
                file = File(demoDir, "Eco_do_Espaco.wav"),
                frequency1 = 293.66, // D4
                frequency2 = 349.23, // F4
                durationSeconds = 12.0
            )
            createSynthWav(
                file = File(demoDir, "Pulso_Eletronico.wav"),
                frequency1 = 220.0, // A3
                frequency2 = 329.63, // E4
                durationSeconds = 18.0
            )
            createSynthWav(
                file = File(demoDir, "Melodia_Curativa.wav"),
                frequency1 = 523.25, // C5
                frequency2 = 659.25, // E5
                durationSeconds = 14.0
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error generating synth files: ${e.message}", e)
            false
        }
    }

    private fun getWavDurationMs(file: File): Long {
        return try {
            val length = file.length()
            val sampleRate = 44100
            val bitsPerSample = 16
            val channels = 1
            val byteRate = sampleRate * channels * (bitsPerSample / 8)
            val audioDataLength = length - 44
            ((audioDataLength.toDouble() / byteRate) * 1000).toLong()
        } catch (e: Exception) {
            15000L
        }
    }

    private fun createSynthWav(file: File, frequency1: Double, frequency2: Double, durationSeconds: Double) {
        val sampleRate = 44100
        val numSamples = (durationSeconds * sampleRate).toInt()
        val totalDataLen = numSamples * 2
        val totalAudioLen = totalDataLen + 36

        val header = ByteArray(44)
        header[0] = 'R'.toByte() // RIFF
        header[1] = 'I'.toByte()
        header[2] = 'F'.toByte()
        header[3] = 'F'.toByte()
        header[4] = (totalAudioLen and 0xff).toByte()
        header[5] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[6] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[7] = ((totalAudioLen shr 24) and 0xff).toByte()
        header[8] = 'W'.toByte() // WAVE
        header[9] = 'A'.toByte()
        header[10] = 'V'.toByte()
        header[11] = 'E'.toByte()
        header[12] = 'f'.toByte() // fmt
        header[13] = 'm'.toByte()
        header[14] = 't'.toByte()
        header[15] = ' '.toByte()
        header[16] = 16 // Subchunk1Size
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // AudioFormat = 1 (PCM)
        header[21] = 0
        header[22] = 1 // NumChannels = 1 (Mono)
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        val byteRate = sampleRate * 2
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = 2 // BlockAlign
        header[33] = 0
        header[34] = 16 // BitsPerSample
        header[35] = 0
        header[36] = 'd'.toByte() // data
        header[37] = 'a'.toByte()
        header[38] = 't'.toByte()
        header[39] = 'a'.toByte()
        header[40] = (totalDataLen and 0xff).toByte()
        header[41] = ((totalDataLen shr 8) and 0xff).toByte()
        header[42] = ((totalDataLen shr 16) and 0xff).toByte()
        header[43] = ((totalDataLen shr 24) and 0xff).toByte()

        file.outputStream().use { out ->
            out.write(header)
            val data = ShortArray(numSamples)
            val fadeCount = (sampleRate * 1.5).toInt() // Fade-in / Fade-out range of 1.5 seconds

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                
                // Generates ambient, soothing space sounds
                // Linear volume envelope (Fade in at beginning, decay at trailing edge)
                val fadeIn = if (i < fadeCount) i.toDouble() / fadeCount else 1.0
                val fadeOut = if (numSamples - i < fadeCount) (numSamples - i).toDouble() / fadeCount else 1.0
                val envelope = fadeIn * fadeOut

                // Alternates melody over time to make actual music
                val speed = t / durationSeconds
                val melodyFreq = when {
                    speed < 0.25 -> frequency1
                    speed < 0.50 -> frequency2
                    speed < 0.75 -> frequency1 * 1.2 // fifth/fourth-ish intervals
                    else -> frequency2 * 1.25
                }

                // Complex beautiful synthesis
                // Base Sine + Triangle overtone + LFO pulse
                val lfo = 1.0 + 0.15 * Math.sin(2.0 * Math.PI * 3.5 * t)
                val baseWave = Math.sin(2.0 * Math.PI * melodyFreq * t)
                val triWave = Math.abs((t * melodyFreq * 2) % 2 - 1) * 2 - 1
                
                val wave = (baseWave * 0.7 + triWave * 0.3) * lfo
                val amplitude = wave * 32767.0 * envelope * 0.45

                data[i] = Math.max(-32768, Math.min(32767, amplitude.toInt())).toShort()
            }

            val buffer = ByteArray(numSamples * 2)
            for (i in 0 until numSamples) {
                val v = data[i]
                buffer[i * 2] = (v.toInt() and 0xff).toByte()
                buffer[i * 2 + 1] = ((v.toInt() shr 8) and 0xff).toByte()
            }
            out.write(buffer)
        }
    }
}
