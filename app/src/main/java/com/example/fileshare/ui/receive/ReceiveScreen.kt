package com.example.fileshare.ui.receive

import android.content.ContentValues
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.platform.LocalContext
import java.net.ServerSocket
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import android.net.wifi.WifiManager
import android.content.Context
import android.provider.MediaStore
import java.net.Socket
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults

fun getBroadcastAddress(context: Context): InetAddress {
    val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    val dhcp = wifi.dhcpInfo

    val broadcast = (dhcp.ipAddress and dhcp.netmask) or dhcp.netmask.inv()

    val quads = ByteArray(4)
    for (k in 0..3) {
        quads[k] = ((broadcast shr k * 8) and 0xFF).toByte()
    }

    return InetAddress.getByAddress(quads)
}
var serverSocketGlobal: ServerSocket? = null
var isBroadcasting = false
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveScreen(navController: NavController) {
    var incomingFileName by remember { mutableStateOf("") }
    var incomingFileSize by remember { mutableStateOf("") }
    var incomingFileType by remember { mutableStateOf("") }
    var isRunning by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var status by remember { mutableStateOf("Idle") }

    // incoming request state
    var showDialog by remember { mutableStateOf(false) }
    var pendingSocket by remember { mutableStateOf<java.net.Socket?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            serverSocketGlobal?.close()
            serverSocketGlobal = null
            isBroadcasting = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Receive File") },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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

                        Text("Turn ON Hotspot")

                        Button(onClick = {
                            val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
                            context.startActivity(intent)
                        }) {
                            Text("Open Settings")
                        }

                        Button(onClick = {
                            if (!isRunning) {
                                isRunning = true
                                status = "Starting..."
                                startServer(
                                    onStatus = { status = it },
                                    onClientConnected = { socket, name, size, type ->
                                        if (!showDialog) {
                                            pendingSocket = socket
                                            incomingFileName = name
                                            incomingFileSize = size
                                            incomingFileType = type
                                            showDialog = true
                                        }
                                    }
                                )
                                startBroadcast(context)
                            }
                        }) {
                            Text("Start Receiving")
                        }

                        Text("Status: $status")
                    }
                }
            }
        }

        // CONFIRMATION DIALOG
        if (showDialog && pendingSocket != null) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Incoming File") },
                text = { Text(
                    "File: $incomingFileName\n" +
                            "Type: .$incomingFileType\n" +
                            "Size: $incomingFileSize bytes\n\n" +
                            "Do you want to download?"
                ) },
                confirmButton = {
                    Button(onClick = {
                        showDialog = false

                        receiveFile(pendingSocket!!, context, incomingFileName,incomingFileType) { status = it }
                    }) {
                        Text("Accept")
                    }
                },
                dismissButton = {
                    Button(onClick = {
                        showDialog = false
                        pendingSocket?.close()
                        status = "Transfer Rejected"
                    }) {
                        Text("Reject")
                    }
                }
            )
        }
    }
}
fun receiveFile(
    socket: Socket,
    context: Context,
    fileName: String,
    fileType: String,
    updateStatus: (String) -> Unit
) {
    Thread {
        try {
            updateStatus("Receiving file...")

            val inputStream = socket.getInputStream()
            val finalName = if (fileName.contains(".")) fileName else "$fileName.$fileType"

            val resolver = context.contentResolver
            val mimeType = when (fileType.lowercase()) {
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "mp3" -> "audio/mpeg"
                "wav" -> "audio/wav"
                "pdf" -> "application/pdf"
                "txt" -> "text/plain"
                "mp4" -> "video/mp4"
                "zip" -> "application/zip"
                "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                else -> "*/*"
            }
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, finalName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                val path = when {
                    mimeType.startsWith("image") -> "Pictures/FileShareApp"
                    mimeType.startsWith("audio") -> "Music/FileShareApp"
                    mimeType.startsWith("video") -> "Movies/FileShareApp"
                    else -> "Download/FileShareApp"
                }

                put(MediaStore.MediaColumns.RELATIVE_PATH, path)
            }

            val collection = when {
                mimeType.startsWith("image") -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                mimeType.startsWith("audio") -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                mimeType.startsWith("video") -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                else -> MediaStore.Downloads.EXTERNAL_CONTENT_URI
            }

            val uri = resolver.insert(collection, contentValues)

            val outputStream = resolver.openOutputStream(uri!!)

            inputStream.copyTo(outputStream!!)

            outputStream.close()
            inputStream.close()
            socket.close()


            updateStatus("File saved successfully")
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)

        } catch (e: Exception) {
            updateStatus("Error: ${e.message}")
        }
    }.start()
}

// SERVER (receives file)
fun startServer(
    onStatus: (String) -> Unit,
    onClientConnected: (Socket, String, String, String) -> Unit
) {
    Thread {
        try {

            if (serverSocketGlobal != null && !serverSocketGlobal!!.isClosed) {
                onStatus("Server already running")
                return@Thread
            }

            serverSocketGlobal = ServerSocket(9999)

            onStatus("Server started... waiting")

            while (true) {
                val socket = serverSocketGlobal?.accept() ?: return@Thread
                onStatus("Incoming connection...")
                val inputStream = socket.getInputStream()
                val reader = inputStream.bufferedReader()
                val metaLine = reader.readLine()
                val parts = metaLine.split("|")

                val fileName = parts[0]
                val fileSize = parts[1]
                val fileType = parts[2]

// pass everything
                onClientConnected(socket, fileName, fileSize, fileType)
            }

        } catch (e: Exception) {
            onStatus("Error: ${e.message}")
        }
    }.start()
}
// BROADCAST (makes device visible to sender)
fun startBroadcast(context: Context) {
    isBroadcasting = true

    Thread {
        try {
            val socket = DatagramSocket(null)
            socket.reuseAddress = true
            socket.bind(java.net.InetSocketAddress(8889))
            socket.broadcast = true

            val message = android.os.Build.MODEL
            val buffer = message.toByteArray()
            val address = getBroadcastAddress(context)

            while (isBroadcasting) {
                val packet = DatagramPacket(buffer, buffer.size, address, 8888)
                socket.send(packet)
                Thread.sleep(2000)
            }

            socket.close()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }.start()
}