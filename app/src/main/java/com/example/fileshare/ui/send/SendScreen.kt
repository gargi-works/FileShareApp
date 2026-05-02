package com.example.fileshare.ui.send

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import java.net.Socket
import java.net.DatagramPacket
import java.net.DatagramSocket
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults

// Get file name
fun getFileName(uri: Uri, context: android.content.Context): String {
    var name = "Unknown"
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index != -1 && it.moveToFirst()) {
            name = it.getString(index)
        }
    }
    return name
}

// UDP LISTENER (find nearby devices)
fun listenForDevices(onDeviceFound: (String, String) -> Unit) {
    Thread {
        try {
            val socket = DatagramSocket(null)
            socket.reuseAddress = true
            socket.bind(java.net.InetSocketAddress(8888))

            println("👂 Listening for devices...")

            while (true) {
                val buffer = ByteArray(1024)
                val packet = DatagramPacket(buffer, buffer.size)

                socket.receive(packet)

                val message = String(packet.data, 0, packet.length)
                val senderIP = packet.address.hostAddress

                println("📡 Found device: $message ($senderIP)")

                onDeviceFound(message, senderIP)
            }

        } catch (e: Exception) {
            println("❌ Listener error: ${e.message}")
            e.printStackTrace()
        }
    }.start()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable

fun SendScreen(navController: NavController) {

    val context = LocalContext.current

    var selectedFile by remember { mutableStateOf<Uri?>(null) }
    var debugText by remember { mutableStateOf("Listening...") }
    // Device list (name + IP)
    var deviceList by remember { mutableStateOf(listOf<Pair<String, String>>()) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedFile = uri
    }

    // Start listening for receivers
    LaunchedEffect(Unit) {
        listenForDevices { name, ip ->
            debugText = "Found: $name"
            if (deviceList.none { it.second == ip }) {
                deviceList = deviceList + (name to ip)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Send File") },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF0F2027),
                            Color(0xFF203A43),
                            Color(0xFF2C5364)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center
            ) {

                Card(
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {

                        Button(
                            onClick = { launcher.launch("*/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Select File")
                        }

                        selectedFile?.let {
                            Text("Selected: ${getFileName(it, context)}")
                        }

                        Text("Nearby Devices:")
                        Text(debugText)

                        deviceList.forEach { (name, ip) ->
                            OutlinedButton(
                                onClick = {
                                    selectedFile?.let {
                                        sendFile(it, context, ip)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(name)
                            }
                        }
                    }
                }
            }
        }

    }
}

// SEND FILE
fun sendFile(uri: Uri, context: android.content.Context, ip: String) {
    Thread {
        try {
            val socket = Socket(ip, 9999)

            val outputStream = socket.getOutputStream()
            val inputStream = context.contentResolver.openInputStream(uri)

            val fileName = getFileName(uri, context)
            val fileSize = inputStream?.available() ?: 0
            val fileType = fileName.substringAfterLast('.', "unknown")

            // SEND METADATA FIRST
            val meta = "$fileName|$fileSize|$fileType\n"
            outputStream.write(meta.toByteArray())
            outputStream.flush()

            Thread.sleep(500) // small delay

            // SEND FILE
            inputStream?.copyTo(outputStream)

            outputStream.flush()
            socket.close()
            inputStream?.close()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }.start()
}