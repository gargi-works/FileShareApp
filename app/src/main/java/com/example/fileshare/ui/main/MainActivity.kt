package com.example.fileshare.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.fileshare.ui.theme.FileShareAppTheme
import androidx.navigation.compose.*
import com.example.fileshare.ui.send.SendScreen
import com.example.fileshare.ui.receive.ReceiveScreen


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FileShareAppTheme {

                val navController = rememberNavController()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(innerPadding)
                    ) {

                        composable("home") {
                            HomeScreen(navController)
                        }

                        composable("send") {
                            SendScreen(navController)
                        }

                        composable("receive") {
                            ReceiveScreen(navController)
                        }
                    }
                }
            }
        }
    }
}

