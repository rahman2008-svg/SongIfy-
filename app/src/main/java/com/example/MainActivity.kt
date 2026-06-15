package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.ui.MusicViewModel
import com.example.ui.theme.*
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    private val viewModel: MusicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: MusicViewModel) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf("songs") }
    var showFullPlayer by remember { mutableStateOf(false) }

    // State bindings
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val scanResultCount by viewModel.scanResultCount.collectAsStateWithLifecycle()

    // Permissions check & request
    val requiredPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.scanMusic(context)
        } else {
            Toast.makeText(
                context,
                "Permission denied. Using high-quality offline streams.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    LaunchedEffect(scanResultCount) {
        scanResultCount?.let { count ->
            Toast.makeText(
                context,
                "Scan complete! Found $count songs on device.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column {
                // Mini Player pinned directly above the main system navigation bar
                MiniPlayerBar(
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    onTogglePlay = { viewModel.togglePlayPause() },
                    onSkipNext = { viewModel.playerHandler.skipToNext() },
                    onExpand = { showFullPlayer = true }
                )
                
                // Navigation Bar
                NavigationBar(
                    containerColor = ObsidianDark,
                    tonalElevation = 8.dp,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    NavigationBarItem(
                        selected = activeTab == "songs",
                        onClick = { activeTab = "songs" },
                        icon = { Icon(Icons.Filled.LibraryMusic, contentDescription = "Tracks") },
                        label = { Text("Songs", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonCyan,
                            selectedTextColor = NeonCyan,
                            indicatorColor = BorderGlass,
                            unselectedTextColor = TextGray,
                            unselectedIconColor = TextGray
                        ),
                        modifier = Modifier.testTag("nav_tab_songs")
                    )
                    NavigationBarItem(
                        selected = activeTab == "playlists",
                        onClick = { activeTab = "playlists" },
                        icon = { Icon(Icons.Filled.QueueMusic, contentDescription = "Playlists") },
                        label = { Text("Playlists", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonCyan,
                            selectedTextColor = NeonCyan,
                            indicatorColor = BorderGlass,
                            unselectedTextColor = TextGray,
                            unselectedIconColor = TextGray
                        ),
                        modifier = Modifier.testTag("nav_tab_playlists")
                    )
                    NavigationBarItem(
                        selected = activeTab == "equalizer",
                        onClick = { activeTab = "equalizer" },
                        icon = { Icon(Icons.Filled.Equalizer, contentDescription = "Equalizer") },
                        label = { Text("Equalizer", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonCyan,
                            selectedTextColor = NeonCyan,
                            indicatorColor = BorderGlass,
                            unselectedTextColor = TextGray,
                            unselectedIconColor = TextGray
                        ),
                        modifier = Modifier.testTag("nav_tab_equalizer")
                    )
                    NavigationBarItem(
                        selected = activeTab == "about",
                        onClick = { activeTab = "about" },
                        icon = { Icon(Icons.Filled.Info, contentDescription = "About Developer") },
                        label = { Text("About", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonCyan,
                            selectedTextColor = NeonCyan,
                            indicatorColor = BorderGlass,
                            unselectedTextColor = TextGray,
                            unselectedIconColor = TextGray
                        ),
                        modifier = Modifier.testTag("nav_tab_about")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(
                targetState = activeTab,
                animationSpec = tween(durationMillis = 300),
                label = "TabTransition"
            ) { tab ->
                when (tab) {
                    "songs" -> SongsTabScreen(
                        viewModel = viewModel,
                        onRequestScan = {
                            val status = ContextCompat.checkSelfPermission(context, requiredPermission)
                            if (status == PackageManager.PERMISSION_GRANTED) {
                                viewModel.scanMusic(context)
                            } else {
                                launcher.launch(requiredPermission)
                            }
                        }
                    )
                    "playlists" -> PlaylistsTabScreen(viewModel = viewModel)
                    "equalizer" -> EqualizerTabScreen(viewModel = viewModel)
                    "about" -> AboutTabScreen()
                }
            }

            // High-End Full Screen Overlay Player
            AnimatedVisibility(
                visible = showFullPlayer && currentSong != null,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy)
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy)
                ) + fadeOut()
            ) {
                currentSong?.let { song ->
                    FullPlayerScreen(
                        song = song,
                        viewModel = viewModel,
                        onDismiss = { showFullPlayer = false }
                    )
                }
            }
        }
    }
}

// FORMAT TIME (MM:SS)
fun formatDuration(ms: Long): String {
    val sec = (ms / 1000) % 60
    val min = (ms / 1000) / 60
    return String.format("%02d:%02d", min, sec)
}

// ----------------------------------------------------
// MINI PLAYER BAR
// ----------------------------------------------------
@Composable
fun MiniPlayerBar(
    currentSong: Song?,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onSkipNext: () -> Unit,
    onExpand: () -> Unit
) {
    if (currentSong == null) return

    val infiniteTransition = rememberInfiniteTransition(label = "ArtRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "VinylDisc"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable { onExpand() }
            .testTag("mini_player"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rotating Album Art Disc
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .rotate(if (isPlaying) rotation else 0f)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(NeonCyan, ElectricBlue)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.LibraryMusic,
                    contentDescription = null,
                    tint = SpaceBlack,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = currentSong.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = currentSong.artist,
                    color = TextGray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onTogglePlay,
                modifier = Modifier.testTag("mini_play_pause")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = NeonCyan,
                    modifier = Modifier.size(28.dp)
                )
            }

            IconButton(
                onClick = onSkipNext,
                modifier = Modifier.testTag("mini_skip_next")
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Next",
                    tint = OnSurfaceWhite,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

// ----------------------------------------------------
// TAB 1: SONGS (CATALOG & SCANNING)
// ----------------------------------------------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongsTabScreen(
    viewModel: MusicViewModel,
    onRequestScan: () -> Unit
) {
    val songs by viewModel.filteredSongs.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()

    var showPlaylistDialog by remember { mutableStateOf<Song?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // App Launcher Brand Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.LibraryMusic,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "SongIfy",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            }

            // Dynamic Scan Local Button
            Button(
                onClick = onRequestScan,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isScanning) BorderGlass else VibrantGreen,
                    contentColor = SpaceBlack
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                enabled = !isScanning,
                modifier = Modifier.testTag("scan_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            color = NeonCyan,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(end = 4.dp),
                            strokeWidth = 2.dp
                        )
                        Text("Scanning...", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            tint = SpaceBlack,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Scan Files", fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        // Search Bar Block
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.searchSongs(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag("search_bar"),
            placeholder = { Text("Search songs, artists, albums...", color = TextGray) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextGray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = OnSurfaceWhite,
                focusedContainerColor = ObsidianDark,
                unfocusedContainerColor = ObsidianDark,
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = BorderGlass
            ),
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Music Tracks Catalog list
        if (songs.isEmpty() && !isScanning) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        Icons.Filled.LibraryMusic,
                        contentDescription = null,
                        tint = TextGray,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        "No Songs Scanned yet",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Scan local files using the button above or play loaded offline audio loops immediately.",
                        color = TextGray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(songs, key = { it.id }) { song ->
                    SongCatalogItem(
                        song = song,
                        onSongClick = { viewModel.playSong(song, songs) },
                        onFavoriteToggle = { viewModel.toggleFavorite(song) },
                        onAddToPlaylist = { showPlaylistDialog = song }
                    )
                }
            }
        }
    }

    // Playlist Picker Dialog
    if (showPlaylistDialog != null) {
        AlertDialog(
            onDismissRequest = { showPlaylistDialog = null },
            title = { Text("Add to Playlist", fontWeight = FontWeight.Bold, color = Color.White) },
            containerColor = ObsidianDark,
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (playlists.isEmpty()) {
                        Text("Create a playlist first from the 'Playlists' tab.", color = TextGray)
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                            items(playlists) { pl ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.addSongToPlaylist(pl.id, showPlaylistDialog!!)
                                            showPlaylistDialog = null
                                        }
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Filled.QueueMusic, contentDescription = null, tint = NeonCyan)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(pl.name, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlaylistDialog = null }) {
                    Text("Close", color = NeonCyan)
                }
            }
        )
    }
}

@Composable
fun SongCatalogItem(
    song: Song,
    onSongClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSongClick() }
            .testTag("song_item_${song.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = if (song.isFavorite) BorderStroke(1.dp, NeonCyan) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Card Thumbnail
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2C2C45)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.LibraryMusic,
                    contentDescription = null,
                    tint = if (song.isFavorite) VibrantGreen else NeonCyan,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (song.isDemo) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ElectricBlue),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                "DEMO",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "${song.artist} • ${song.album}",
                        color = TextGray,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = formatDuration(song.duration),
                color = TextGray,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            // Playlist Add Quick Option
            IconButton(onClick = onAddToPlaylist) {
                Icon(
                    Icons.AutoMirrored.Filled.PlaylistAdd,
                    contentDescription = "Add to playlist",
                    tint = NeonCyan,
                    modifier = Modifier.size(21.dp)
                )
            }

            // Favorites quick toggle
            IconButton(
                onClick = onFavoriteToggle,
                modifier = Modifier.testTag("fav_toggle_${song.id}")
            ) {
                Icon(
                    imageVector = if (song.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (song.isFavorite) FavoriteRed else TextGray,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// ----------------------------------------------------
// TAB 2: PLAYLISTS & FAVORITES MANAGEMENT
// ----------------------------------------------------
@Composable
fun PlaylistsTabScreen(viewModel: MusicViewModel) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteSongs.collectAsStateWithLifecycle()
    val currentSelectedPlaylist by viewModel.currentSelectedPlaylist.collectAsStateWithLifecycle()
    val playlistSongs by viewModel.currentPlaylistSongs.collectAsStateWithLifecycle()

    var newPlaylistName by remember { mutableStateOf("") }

    if (currentSelectedPlaylist != null) {
        // Detailed Playlist Tracks Panel
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.selectPlaylist(null) }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = NeonCyan)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = currentSelectedPlaylist!!.name,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "${playlistSongs.size} Tracks",
                        color = TextGray,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (playlistSongs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No songs inside this playlist yet!\nGo to the Songs tab and tap the + icon.",
                        color = TextGray,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(playlistSongs) { song ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(BorderGlass)
                                        .clickable { viewModel.playSong(song, playlistSongs) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = NeonCyan)
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        song.title,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        fontSize = 14.sp
                                    )
                                    Text(song.artist, color = TextGray, fontSize = 11.sp, maxLines = 1)
                                }

                                IconButton(onClick = {
                                    viewModel.removeSongFromPlaylist(currentSelectedPlaylist!!.id, song)
                                }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = FavoriteRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Main lists of Playlists & Favorites split
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                "My Playlists",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Create playlist row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    placeholder = { Text("New Playlist Name", color = TextGray) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("create_playlist_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = OnSurfaceWhite,
                        focusedContainerColor = ObsidianDark,
                        unfocusedContainerColor = ObsidianDark,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = BorderGlass
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            viewModel.createPlaylist(newPlaylistName)
                            newPlaylistName = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = SpaceBlack),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("create_playlist_button")
                ) {
                    Text("Create", fontWeight = FontWeight.Black)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Scrollable section for playlists
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Favorites Category (Sticky static list)
                item {
                    Text(
                        "Favorites & Hearts",
                        color = FavoriteRed,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Create a dummy active playlist for Favorites
                                val mockPlaylist = Playlist(id = -99L, name = "Favorites & Loved", songCount = favorites.size)
                                viewModel.selectPlaylist(mockPlaylist)
                            }
                            .testTag("favorite_playlist"),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(FavoriteRed.copy(0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Favorite, contentDescription = null, tint = FavoriteRed)
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Loved Tracks", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                Text("${favorites.size} Hearts", color = TextGray, fontSize = 12.sp)
                            }
                            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextGray)
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Custom Folders",
                        color = NeonCyan,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                if (playlists.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No custom playlists created yet.", color = TextGray, fontSize = 13.sp)
                        }
                    }
                } else {
                    items(playlists) { pl ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectPlaylist(pl) }
                                .testTag("playlist_item_${pl.id}"),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(BorderGlass),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.QueueMusic, contentDescription = null, tint = NeonCyan)
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(pl.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("Local Playlist", color = TextGray, fontSize = 11.sp)
                                }

                                IconButton(onClick = { viewModel.deletePlaylist(pl) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = FavoriteRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// TAB 3: EQUALIZER SCREEN
// ----------------------------------------------------
@Composable
fun EqualizerTabScreen(viewModel: MusicViewModel) {
    val isEnabled by viewModel.equalizerEnabled.collectAsStateWithLifecycle()
    val bands by viewModel.equalizerBands.collectAsStateWithLifecycle()

    val bandFrequencies = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Studio Equalizer",
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
        Text(
            "Optimize acoustic performance and audio frequencies",
            color = TextGray,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // On Off Toggle Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isEnabled) SurfaceCard else ObsidianDark
            ),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, if (isEnabled) NeonCyan else BorderGlass)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Dolby Digital FX Core",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                    Text("Enable high dynamic hardware filters", color = TextGray, fontSize = 11.sp)
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = { viewModel.playerHandler.toggleEqualizer(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SpaceBlack,
                        checkedTrackColor = NeonCyan,
                        uncheckedThumbColor = TextGray,
                        uncheckedTrackColor = SurfaceCard
                    ),
                    modifier = Modifier.testTag("eq_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Interactive 5 Band Layout Slider Deck
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, BorderGlass)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Decibel Levels (dB)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Render 5 vertical frequency sliders side by side
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    bands.forEachIndexed { i, dbValue ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            Text(
                                text = if (dbValue >= 0) "+$dbValue" else "$dbValue",
                                color = if (isEnabled) NeonCyan else TextGray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )

                            // Vertical Slider using a rotated Box
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Slider(
                                    value = dbValue.toFloat(),
                                    onValueChange = { newValue ->
                                        if (isEnabled) {
                                            viewModel.playerHandler.updateEqualizerBand(i, newValue.toInt())
                                        }
                                    },
                                    valueRange = -15f..15f,
                                    steps = 30,
                                    enabled = isEnabled,
                                    colors = SliderDefaults.colors(
                                        thumbColor = if (isEnabled) VibrantGreen else TextGray,
                                        activeTrackColor = if (isEnabled) NeonCyan else BorderGlass,
                                        inactiveTrackColor = BorderGlass
                                    ),
                                    modifier = Modifier
                                        .graphicsLayer {
                                            rotationZ = -90f
                                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center
                                        }
                                        .width(130.dp)
                                )
                            }

                            Text(
                                text = bandFrequencies.getOrElse(i) { "Band" },
                                color = if (isEnabled) OnSurfaceWhite else TextGray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                HorizontalDivider(color = BorderGlass)

                Spacer(modifier = Modifier.height(10.dp))

                // Presets Segment Grid
                Text(
                    "Equalizer Quick Presets",
                    color = TextGray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                val presetNames = listOf("Classical", "Bass Power", "Vocal Center", "Electro", "Flat Studio")
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(presetNames.size) { index ->
                        Card(
                            modifier = Modifier
                                .clickable(enabled = isEnabled) {
                                    viewModel.playerHandler.applyPreset(index)
                                }
                                .testTag("preset_$index"),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (!isEnabled) BorderGlass else SurfaceCard
                            ),
                            border = BorderStroke(1.dp, if (isEnabled) NeonCyan.copy(0.4f) else BorderGlass)
                        ) {
                            Text(
                                presetNames[index],
                                color = if (isEnabled) Color.White else TextGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// TAB 4: ABOUT PRESTIGE & CORNER DEV INFO
// ----------------------------------------------------
@Composable
fun AboutTabScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // App Identity Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianDark),
                border = BorderStroke(1.dp, NeonCyan)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(NeonCyan, SpaceBlack))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.LibraryMusic, contentDescription = null, tint = SpaceBlack, modifier = Modifier.size(36.dp))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "SongIfy Player",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "Prestige Edition • v1.0.0",
                        color = VibrantGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "A beautiful, lightning-fast offline music environment developed with modular Room structures and native ExoAudio DSP models.",
                        color = TextGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // About Developer Section
        item {
            Text(
                "About Developer",
                color = NeonCyan,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                letterSpacing = 0.5.sp
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    Text(
                        "Prince AR Abdur Rahman",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                    Text(
                        "Independent App Developer",
                        color = VibrantGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Independent App Developer passionate about building modern Android applications, productivity tools, AI-powered experiences, media players, educational apps, and next-generation digital products.",
                        color = TextGray,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Direct Line Context:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row {
                        Icon(Icons.Filled.Phone, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("WhatsApp: 01707424006 / 01796951709", color = OnSurfaceWhite, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row {
                        Icon(Icons.Filled.AccountTree, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Facebook: facebook.com/share/1BNn32qoJo/", color = OnSurfaceWhite, fontSize = 12.sp)
                    }
                }
            }
        }

        // About Company Section
        item {
            Text(
                "About Publisher",
                color = NeonCyan,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    Text(
                        "NexVora Lab's Ofc",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "NexVora Lab's Ofc focuses on creating innovative Android applications designed to improve productivity, entertainment, learning, and digital experiences.",
                        color = TextGray,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Mission: Build fast, beautiful, privacy-friendly, and user-focused applications accessible to everyone.",
                        color = OnSurfaceWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Company Products Grid header
        item {
            Text(
                "NexVora Lab Pro Collection",
                color = NeonCyan,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp
            )
        }

        // Product chips grid list
        item {
            val products = listOf(
                "NexPlay X", "LifeSphere OS", "Smart Day Planner X", "Study AI",
                "Lensora Studio", "Offline AI", "NexVora Love Space", "CalcVerse", "NexVoice OS"
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .height(210.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(products) { prod ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ObsidianDark),
                        border = BorderStroke(1.dp, BorderGlass),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(VibrantGreen)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                prod,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Core credits
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "© 2026 NexVora Lab's Ofc. All Rights Reserved.\nPublished by NexVora, crafted under extreme premium constraints.",
                color = TextGray,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            )
        }
    }
}

// ----------------------------------------------------
// FULL SCREEN SCREEN PLAYER
// ----------------------------------------------------
@Composable
fun FullPlayerScreen(
    song: Song,
    viewModel: MusicViewModel,
    onDismiss: () -> Unit
) {
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentPosition by viewModel.currentPosition.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsStateWithLifecycle()
    val isRepeatEnabled by viewModel.isRepeatEnabled.collectAsStateWithLifecycle()

    var playbackSpeed by remember { mutableStateOf(1.0f) }

    // Vinyl Disc Animation
    val infiniteTransition = rememberInfiniteTransition(label = "VinylDisc")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "VinylDiscPlay"
    )

    // Glowing Equalizer Wave Animation
    val pulseScale by rememberInfiniteTransition(label = "RadarPulse").animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RadarAnimation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SpaceBlack, ObsidianDark)
                )
            )
            .padding(24.dp)
            .testTag("full_player")
    ) {
        // Top row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_player")
            ) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Minimize", tint = Color.White, modifier = Modifier.size(32.dp))
            }

            Text(
                "NOW SPINNING",
                color = VibrantGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )

            IconButton(onClick = { viewModel.toggleFavorite(song) }) {
                Icon(
                    imageVector = if (song.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (song.isFavorite) FavoriteRed else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Center Rotating Vinyl Deck Art
        Box(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Pulse Aura
            Box(
                modifier = Modifier
                    .size(290.dp)
                    .graphicsLayer {
                        scaleX = if (isPlaying) pulseScale else 1.0f
                        scaleY = if (isPlaying) pulseScale else 1.0f
                    }
                    .clip(RoundedCornerShape(36.dp))
                    .background(SoftLavender.copy(0.08f))
            )

            // Sophisticated Rounded Album Art Container
            Box(
                modifier = Modifier
                    .size(270.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(CharcoalDark, DeepPurpleAccent)
                        )
                    )
                    .border(BorderStroke(1.dp, Color.White.copy(0.08f)), RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Blur mesh effect simulation
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = 0.45f }
                ) {
                    Box(
                        modifier = Modifier
                            .offset(x = (-40).dp, y = (-40).dp)
                            .size(160.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(SoftLavender, Color.Transparent)))
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 40.dp, y = 40.dp)
                            .size(160.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(SoftRose, Color.Transparent)))
                    )
                }

                // Rotating Vinyl insert
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .rotate(if (isPlaying) rotation else 0f)
                        .clip(CircleShape)
                        .background(Color(0xFF0F0E13))
                        .border(BorderStroke(3.dp, Color.White.copy(0.12f)), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // Ridges
                    Canvas(modifier = Modifier.size(140.dp)) {
                        drawCircle(color = Color.White.copy(alpha = 0.08f), radius = size.minDimension / 2, style = androidx.compose.ui.graphics.drawscope.Stroke(3f))
                        drawCircle(color = Color.White.copy(alpha = 0.05f), radius = size.minDimension / 3, style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
                        drawCircle(color = Color.White.copy(alpha = 0.03f), radius = size.minDimension / 4, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f))
                    }

                    // Frosted middle disc
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(0.06f))
                            .border(BorderStroke(1.dp, Color.White.copy(0.15f)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.sweepGradient(
                                        colors = listOf(SoftLavender, SoftRose, DeepPurpleAccent, SoftLavender)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.MusicNote,
                                contentDescription = null,
                                tint = DeepPurpleAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Lyrics / Track Info Center
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = song.title,
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${song.artist} • ${song.album}",
                color = TextGray,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Seek Progress slider layout
        Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = currentPosition.toFloat(),
                onValueChange = { viewModel.playerHandler.seekTo(it.toLong()) },
                valueRange = 0f..(duration.toFloat().coerceAtLeast(1f)),
                colors = SliderDefaults.colors(
                    thumbColor = NeonCyan,
                    activeTrackColor = NeonCyan,
                    inactiveTrackColor = BorderGlass
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDuration(currentPosition),
                    color = TextGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "-" + formatDuration((duration - currentPosition).coerceAtLeast(0L)),
                    color = TextGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action Deck Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle
            IconButton(onClick = { viewModel.playerHandler.toggleShuffle() }) {
                Icon(
                    Icons.Filled.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (isShuffleEnabled) NeonCyan else TextGray,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Previous
            IconButton(onClick = { viewModel.playerHandler.skipToPrevious() }) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = OnSurfaceWhite, modifier = Modifier.size(34.dp))
            }

            // Central Pulsing Play FAB
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(VibrantGreen)
                    .clickable { viewModel.togglePlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "PlayPause",
                    tint = SpaceBlack,
                    modifier = Modifier.size(40.dp)
                )
            }

            // Next
            IconButton(onClick = { viewModel.playerHandler.skipToNext() }) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = OnSurfaceWhite, modifier = Modifier.size(34.dp))
            }

            // Repeat
            IconButton(onClick = { viewModel.playerHandler.toggleRepeat() }) {
                Icon(
                    Icons.Filled.Repeat,
                    contentDescription = "Repeat",
                    tint = if (isRepeatEnabled) NeonCyan else TextGray,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Playback Speed Slider Controller
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Speed / Tempo Control",
                    color = TextGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${String.format("%.1f", playbackSpeed)}x",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Slider(
                value = playbackSpeed,
                onValueChange = {
                    playbackSpeed = it
                    viewModel.playerHandler.exoPlayer.setPlaybackSpeed(it)
                },
                valueRange = 0.5f..2.0f,
                steps = 15,
                colors = SliderDefaults.colors(
                    thumbColor = ElectricBlue,
                    activeTrackColor = ElectricBlue,
                    inactiveTrackColor = BorderGlass
                )
            )
        }
    }
}
