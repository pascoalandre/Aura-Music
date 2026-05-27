package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.data.MusicRepository
import com.example.data.database.AppDatabase
import com.example.ui.MusicViewModel
import com.example.ui.screens.MainMusicScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Support immersive Edge-To-Edge safe areas
        enableEdgeToEdge()

        // Initialize dependencies securely
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = MusicRepository(applicationContext, database.playlistDao())
        
        // Instantiate ViewModel
        val viewModel: MusicViewModel by viewModels {
            MusicViewModel.Factory(application, repository)
        }

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    MainMusicScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
