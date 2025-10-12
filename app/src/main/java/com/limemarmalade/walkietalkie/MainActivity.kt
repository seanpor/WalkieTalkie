package com.limemarmalade.walkietalkie

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.limemarmalade.walkietalkie.ui.theme.WalkietalkieTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionsLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.all { it.value }
            if (allGranted) {
                // All permissions granted, can proceed
            } else {
                // Show error message
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WalkietalkieTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WalkieTalkieApp(viewModel, onPermissionRequested = { mode ->
                        val permissionsToRequest = mutableListOf<String>()

                        // Audio recording permission
                        if (ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.RECORD_AUDIO
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
                        }

                        // Notification permission (Android 13+)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                        }

                        // Foreground service permission (Android 14+)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                            ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionsToRequest.add(Manifest.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE)
                        }

                        if (permissionsToRequest.isNotEmpty()) {
                            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
                            // Wait for permissions before starting
                        } else {
                            // All permissions granted, start immediately
                            viewModel.onModeSelected(mode)
                        }
                    })
                }
            }
        }
    }
}

@Composable
fun WalkieTalkieApp(viewModel: MainViewModel, onPermissionRequested: (Mode) -> Unit) {
    val screen by viewModel.screen.collectAsState()
    val ipAddress by viewModel.ipAddress.collectAsState()

    when (screen) {
        Screen.Selection -> SelectionScreen(
            onHost = { onPermissionRequested(Mode.HOST) },
            onClient = { onPermissionRequested(Mode.CLIENT) }
        )
        Screen.Host -> HostScreen(ipAddress)
        Screen.Client -> ClientScreen(onConnect = { ip -> viewModel.onConnect(ip) })
        Screen.Connected -> ConnectedScreen()
    }
}

@Composable
fun SelectionScreen(onHost: () -> Unit, onClient: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = onHost) {
            Text("Host")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onClient) {
            Text("Client")
        }
    }
}

@Composable
fun HostScreen(ipAddress: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Your IP Address:")
        Text(ipAddress, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Waiting for clients to connect...")
    }
}

@Composable
fun ClientScreen(onConnect: (String) -> Unit) {
    var ipAddress by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            value = ipAddress,
            onValueChange = { ipAddress = it },
            label = { Text("Host IP Address") },
            placeholder = { Text("e.g. 192.168.1.100") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                keyboardController?.hide()
                onConnect(ipAddress)
            })
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            keyboardController?.hide()
            onConnect(ipAddress)
        }) {
            Text("Connect")
        }
    }
}

@Composable
fun ConnectedScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Connected! You can now talk.", style = MaterialTheme.typography.headlineSmall)
    }
}