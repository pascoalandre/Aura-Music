package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Song
import com.example.data.database.PlaylistEntity
import com.example.ui.MusicViewModel
import com.example.ui.components.EqualizerView
import com.example.ui.components.AlbumArtView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMusicScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allSongs by viewModel.allSongs.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsStateWithLifecycle()
    val playlistSongs by viewModel.playlistSongs.collectAsStateWithLifecycle()
    
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentPosition by viewModel.currentPosition.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    val permissionGranted by viewModel.permissionGranted.collectAsStateWithLifecycle()

    var showAddPlaylistDialog by remember { mutableStateOf(false) }
    var playlistNameInput by remember { mutableStateOf("") }
    
    // Switch between "Músicas" and "Playlists"
    var activeTab by remember { mutableStateOf(0) } // 0 = Songs, 1 = Playlists
    
    // Playlist Song Editor state
    var editingPlaylistSongs by remember { mutableStateOf<PlaylistEntity?>(null) }

    // Big Player overlay expansion
    var isPlayerExpanded by remember { mutableStateOf(false) }

    // Permission launcher
    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setPermissionGranted(granted)
    }

    // Launch initial permission state check on startup
    LaunchedEffect(Unit) {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        viewModel.setPermissionGranted(hasPermission)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column(
                modifier = Modifier
                    .background(Color(0xFF0D1117))
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                // Header of Sinfonia App
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "AURA",
                            style = LocalTextStyle.current.copy(
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF00E676), Color(0xFF1DB954))
                                ),
                                shadow = Shadow(
                                    color = Color(0x6600E676),
                                    offset = Offset(0f, 4f),
                                    blurRadius = 8f
                                )
                            ),
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "REPRODUTOR DE MÚSICA",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            letterSpacing = 1.sp
                        )
                    }

                    // Scan button if permission isn't granted yet
                    if (!permissionGranted) {
                        Button(
                            onClick = { launcher.launch(permissionToRequest) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1DB954).copy(alpha = 0.15f),
                                contentColor = Color(0xFF00E676)
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("scan_permission_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = "Escanear músicas",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Escanear", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // Badge indicating Storage was successfully verified
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF00E676).copy(alpha = 0.12f), CircleShape)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Local Integrado",
                                fontSize = 10.sp,
                                color = Color(0xFF00E676),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Standard Segmented Tabs switcher (Músicas | Playlists)
                if (selectedPlaylist == null) {
                    TabRow(
                        selectedTabIndex = activeTab,
                        containerColor = Color.Transparent,
                        contentColor = Color(0xFF00E676),
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                                color = Color(0xFF00E676),
                                height = 3.dp
                            )
                        },
                        divider = {}
                    ) {
                        Tab(
                            selected = activeTab == 0,
                            onClick = { activeTab = 0 },
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Músicas", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            },
                            modifier = Modifier.testTag("songs_tab")
                        )
                        Tab(
                            selected = activeTab == 1,
                            onClick = { activeTab = 1 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Playlists", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            },
                            modifier = Modifier.testTag("playlists_tab")
                        )
                    }
                } else {
                    // Back header when viewing inside a Playlist
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.selectPlaylist(null) },
                            modifier = Modifier.testTag("back_to_lists_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = selectedPlaylist?.name ?: "Playlist",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${playlistSongs.size} músicas",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        // Edit Playlist configuration button
                        Button(
                            onClick = { editingPlaylistSongs = selectedPlaylist },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00E676),
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("edit_playlist_songs_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Músicas", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF0A0D14),
        floatingActionButton = {
            // Floating Action Button to generate instant playlist
            if (activeTab == 1 && selectedPlaylist == null) {
                ExtendedFloatingActionButton(
                    onClick = { showAddPlaylistDialog = true },
                    containerColor = Color(0xFF1DB954),
                    contentColor = Color.Black,
                    shape = RoundedCornerShape(16.dp),
                    icon = { Icon(Icons.Default.Add, "Criar Playlist") },
                    text = { Text("Criar Playlist", fontWeight = FontWeight.Bold) },
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = if (currentSong != null) 72.dp else 0.dp)
                        .testTag("create_playlist_fab")
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (selectedPlaylist != null) {
                    // Inside a specific Playlist
                    if (playlistSongs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = Color.Gray.copy(alpha = 0.5f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Esta playlist está vazia",
                                    color = Color.LightGray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Adicione músicas clicando no botão acima.",
                                    color = Color.Gray,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = { editingPlaylistSongs = selectedPlaylist },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954)),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.testTag("empty_add_songs_button")
                                ) {
                                    Text("Escolher Músicas", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(bottom = 90.dp, top = 10.dp)
                        ) {
                            items(playlistSongs) { song ->
                                SongItemRow(
                                    song = song,
                                    isPlaying = isPlaying && currentSong?.path == song.path,
                                    isSelected = currentSong?.path == song.path,
                                    onClick = { viewModel.selectAndPlay(song, playlistSongs) },
                                    onDelete = {
                                        viewModel.toggleSongInPlaylist(selectedPlaylist!!.id, song, true)
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // Músicas & Playlists browser
                    when (activeTab) {
                        0 -> {
                            // "Músicas" list
                            if (allSongs.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(color = Color(0xFF00E676))
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("Buscando sua biblioteca...", color = Color.Gray)
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    contentPadding = PaddingValues(bottom = 90.dp, top = 10.dp)
                                ) {
                                    item {
                                        Text(
                                            text = "Suas Músicas (${allSongs.size})",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.LightGray,
                                            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
                                        )
                                    }
                                    items(allSongs) { song ->
                                        SongItemRow(
                                            song = song,
                                            isPlaying = isPlaying && currentSong?.path == song.path,
                                            isSelected = currentSong?.path == song.path,
                                            onClick = { viewModel.selectAndPlay(song, allSongs) }
                                        )
                                    }
                                }
                            }
                        }
                        1 -> {
                            // "Playlists" lists
                            if (playlists.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.QueueMusic,
                                            contentDescription = null,
                                            tint = Color.Gray.copy(alpha = 0.5f),
                                            modifier = Modifier.size(72.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "Crie sua primeira playlist",
                                            color = Color.LightGray,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Organize suas melodias favoritas do celular.",
                                            color = Color.Gray,
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(24.dp))
                                        Button(
                                            onClick = { showAddPlaylistDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954)),
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.testTag("initial_create_playlist_button")
                                        ) {
                                            Text("Nova Playlist", color = Color.Black, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    contentPadding = PaddingValues(bottom = 90.dp, top = 10.dp)
                                ) {
                                    items(playlists) { playlist ->
                                        PlaylistItemCard(
                                            playlist = playlist,
                                            onClick = { viewModel.selectPlaylist(playlist) },
                                            onDelete = { viewModel.deletePlaylist(playlist.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Sticky Bottom mini player
            AnimatedVisibility(
                visible = currentSong != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                currentSong?.let { song ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF161B22), Color(0xFF0F141C))
                                )
                            )
                            .clickable { isPlayerExpanded = true }
                            .padding(8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Cover art / Rotating animated disc
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0D1117)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isPlaying) {
                                    EqualizerView(
                                        isPlaying = true,
                                        modifier = Modifier.padding(10.dp),
                                        barCount = 6,
                                        activeColor = Color(0xFF00E676)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = Color(0xFF00E676),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.artist,
                                    color = Color.LightGray,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Standard pause / next commands
                            IconButton(
                                onClick = { viewModel.togglePlayPause() },
                                modifier = Modifier.testTag("mini_play_pause_button")
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Continuar ou Pausar",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            IconButton(
                                onClick = { viewModel.playNext() },
                                modifier = Modifier.testTag("mini_next_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Próxima Música",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal dialog to create new playlist
    if (showAddPlaylistDialog) {
        Dialog(onDismissRequest = { showAddPlaylistDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF161B22),
                tonalElevation = 6.dp,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.QueueMusic,
                        contentDescription = null,
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Nova Playlist",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = playlistNameInput,
                        onValueChange = { playlistNameInput = it },
                        label = { Text("Nome da Playlist") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00E676),
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = Color(0xFF00E676),
                            unfocusedLabelColor = Color.LightGray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("playlist_name_field")
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                playlistNameInput = ""
                                showAddPlaylistDialog = false
                            }
                        ) {
                            Text("Cancelar", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                if (playlistNameInput.isNotBlank()) {
                                    viewModel.createPlaylist(playlistNameInput)
                                    playlistNameInput = ""
                                    showAddPlaylistDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954)),
                            modifier = Modifier.testTag("playlist_save_button")
                        ) {
                            Text("Criar", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Modal checklist for adding and removing songs (Editing Playlist)
    if (editingPlaylistSongs != null) {
        val playlist = editingPlaylistSongs!!
        // Let's obtain the currents paths list in this playlist
        val currentPaths = playlistSongs.map { it.path }.toSet()

        Dialog(onDismissRequest = { editingPlaylistSongs = null }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF0F141C),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Músicas em: ${playlist.name}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    HorizontalDivider(color = Color.DarkGray)

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 12.dp)
                    ) {
                        items(allSongs) { song ->
                            val isAdded = currentPaths.contains(song.path)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        viewModel.toggleSongInPlaylist(playlist.id, song, isAdded)
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isAdded,
                                    onCheckedChange = {
                                        viewModel.toggleSongInPlaylist(playlist.id, song, isAdded)
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00E676))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = song.title,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = song.artist,
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color.DarkGray)

                    Button(
                        onClick = { editingPlaylistSongs = null },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .testTag("editing_playlist_songs_done_button")
                    ) {
                        Text("Pronto", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Full Immense Player Sheet Layer overlay
    if (isPlayerExpanded) {
        currentSong?.let { song ->
            FullPlayerPanel(
                song = song,
                isPlaying = isPlaying,
                positionMs = currentPosition,
                durationMs = duration,
                onSeek = { viewModel.seekTo(it) },
                onTogglePlay = { viewModel.togglePlayPause() },
                onNext = { viewModel.playNext() },
                onPrevious = { viewModel.playPrevious() },
                onDismiss = { isPlayerExpanded = false }
            )
        }
    }
}

@Composable
fun SongItemRow(
    song: Song,
    isPlaying: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() }
            .testTag("song_row_${song.title.replace(" ", "_")}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF161B22) else Color(0xFF0D1117)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Index or mini animation equalizer
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) Color(0xFF00E676).copy(alpha = 0.15f) else Color(0xFF1F2937)),
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
                    EqualizerView(
                        isPlaying = true,
                        modifier = Modifier.padding(10.dp),
                        barCount = 4,
                        activeColor = Color(0xFF00E676)
                    )
                } else {
                    Icon(
                        imageVector = if (song.isDemo) Icons.Default.Favorite else Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = if (isSelected) Color(0xFF00E676) else Color.LightGray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = if (isSelected) Color(0xFF00E676) else Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    color = Color.Gray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = formatTime(song.duration),
                color = Color.Gray,
                fontSize = 12.sp
            )

            if (onDelete != null) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remover",
                        tint = Color(0xFFEF5350),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistItemCard(
    playlist: PlaylistEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable { onClick() }
            .testTag("playlist_card_${playlist.name.replace(" ", "_")}"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF00E676).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.QueueMusic,
                    contentDescription = null,
                    tint = Color(0xFF00E676),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Biblioteca de Músicas",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_playlist_${playlist.name.replace(" ", "_")}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Excluir Playlist",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// Gorgeous full screen media page player
@Composable
fun FullPlayerPanel(
    song: Song,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Dismiss arrow and info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("player_dismiss_button")) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fechar player",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = "REPRODUZINDO AGORA",
                    color = Color.LightGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp
                )
                Box(modifier = Modifier.size(48.dp)) // Equalizer placeholder space
            }

            // Big gorgeous Neon album circle with artwork and integrated visualizer
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color(0xFF00E676), CircleShape)
                    .background(Color(0xFF161B22)),
                contentAlignment = Alignment.Center
            ) {
                // Render Album Artwork
                AlbumArtView(
                    path = song.path,
                    fallbackTitle = song.title,
                    fallbackArtist = song.artist,
                    modifier = Modifier.fillMaxSize()
                )

                // If playing, we draw energetic glowing visualizers as an overlay with glassmorphism blending
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EqualizerView(
                        isPlaying = isPlaying,
                        modifier = Modifier.fillMaxSize(),
                        barCount = 14,
                        activeColor = Color(0xFF00E676),
                        secondaryColor = Color(0xFF1DB954)
                    )
                }
            }

            // Metadata info including Album details, Arist and release year
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = song.title,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = song.artist,
                        color = Color(0xFF00E676),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    if (!song.year.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "•",
                            color = Color.Gray,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = song.year,
                            color = Color.LightGray,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Seeking slide track bar
            Column(modifier = Modifier.fillMaxWidth()) {
                val progressValue = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
                Slider(
                    value = progressValue,
                    onValueChange = { onSeek((it * durationMs).toLong()) },
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00E676),
                        activeTrackColor = Color(0xFF1DB954),
                        inactiveTrackColor = Color.DarkGray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("progress_slider")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = formatTime(positionMs), color = Color.Gray, fontSize = 11.sp)
                    Text(text = formatTime(durationMs), color = Color.Gray, fontSize = 11.sp)
                }
            }

            // Command buttons row (Prev, Play-Pause, Next)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 36.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevious, modifier = Modifier.size(56.dp).testTag("player_prev_button")) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Anterior",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))

                FloatingActionButton(
                    onClick = onTogglePlay,
                    containerColor = Color(0xFF00E676),
                    contentColor = Color.Black,
                    shape = CircleShape,
                    modifier = Modifier.size(72.dp).testTag("player_play_pause_fab")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Continuar ou Pausar",
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))

                IconButton(onClick = onNext, modifier = Modifier.size(56.dp).testTag("player_next_button")) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Próxima",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
