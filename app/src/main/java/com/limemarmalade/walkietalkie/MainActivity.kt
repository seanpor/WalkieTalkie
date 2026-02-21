package com.limemarmalade.walkietalkie

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.limemarmalade.walkietalkie.ui.theme.WalkietalkieTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels { MainViewModelFactory(application) }
    private var walkieTalkieService: WalkieTalkieService? = null
    private var isServiceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as WalkieTalkieService.WalkieTalkieBinder
            walkieTalkieService = binder.getService()
            isServiceBound = true
            lifecycleScope.launch {
                while (isServiceBound) {
                    walkieTalkieService?.getClientCount()?.let {
                        viewModel.updateClientCount(it)
                    }
                    delay(1000) // Poll every second
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            walkieTalkieService = null
            isServiceBound = false
        }
    }

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

    override fun onStart() {
        super.onStart()
        Intent(this, WalkieTalkieService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize billing
        viewModel.initializeBilling(this)
        
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
    val clientCount by viewModel.clientCount.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val productPrice by viewModel.productPrice.collectAsState()
    val purchaseError by viewModel.purchaseError.collectAsState()
    val showPrivacyPolicy by viewModel.showPrivacyPolicy.collectAsState()
    val showTermsOfService by viewModel.showTermsOfService.collectAsState()
    val showSettingsDialog by viewModel.showSettingsDialog.collectAsState()
    val showPurchaseSuccess by viewModel.showPurchaseSuccess.collectAsState()

    // Update product price when billing manager is ready
    LaunchedEffect(Unit) {
        viewModel.getCurrentProductPrice()?.let { price ->
            viewModel.updateProductPrice(price)
        }
    }

    // Show purchase success dialog if needed
    if (showPurchaseSuccess) {
        PurchaseSuccessDialog(
            onContinue = { 
                viewModel.hidePurchaseSuccess()
                viewModel.clearError()
            }
        )
    }

    // Show settings dialog if needed
    if (showSettingsDialog) {
        SettingsDialog(
            onPrivacyPolicyClick = { 
                viewModel.hideSettingsDialog()
                viewModel.showPrivacyPolicy()
            },
            onTermsOfServiceClick = { 
                viewModel.hideSettingsDialog()
                viewModel.showTermsOfService()
            },
            onDismiss = { viewModel.hideSettingsDialog() }
        )
    }

    // Show legal document dialogs if needed
    if (showPrivacyPolicy) {
        LegalDocumentDialog(
            title = "Privacy Policy",
            resourceId = R.raw.privacy_policy,
            onDismiss = { viewModel.hidePrivacyPolicy() }
        )
    }

    if (showTermsOfService) {
        LegalDocumentDialog(
            title = "Terms of Service",
            resourceId = R.raw.terms_of_service,
            onDismiss = { viewModel.hideTermsOfService() }
        )
    }

    when (screen) {
        Screen.Selection -> SelectionScreen(
            onHost = { onPermissionRequested(Mode.HOST) },
            onClient = { onPermissionRequested(Mode.CLIENT) },
            onSettingsClick = { viewModel.showSettingsDialog() }
        )
        Screen.Host -> HostScreen(ipAddress, clientCount)
        Screen.Client -> ClientScreen(ipAddress, onConnect = { ip -> viewModel.onConnect(ip) })
        Screen.Connected -> ConnectedScreen()
        Screen.PurchaseRequired -> PurchaseRequiredScreen(
            productPrice = productPrice,
            onPurchase = { viewModel.startPurchase(it) },
            onRestorePurchases = { viewModel.restorePurchases() },
            onDismiss = { viewModel.setPremiumStatus(false) },
            errorMessage = purchaseError
        )
    }
}

@Composable
fun LegalDocumentDialog(title: String, resourceId: Int, onDismiss: () -> Unit) {
    val context = LocalContext.current
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }
                
                // WebView for HTML content
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            webViewClient = WebViewClient()
                            loadUrl("file:///android_res/raw/$resourceId")
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun PurchaseSuccessDialog(
    onContinue: () -> Unit
) {
    Dialog(onDismissRequest = {}) { // Prevent dismiss by clicking outside
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .width(300.dp)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Success icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            MaterialTheme.shapes.medium
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    "Purchase Successful!",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    "Thank you for supporting WalkieTalkie!",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "You now have access to all premium features.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Continue")
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(
    onPrivacyPolicyClick: () -> Unit,
    onTermsOfServiceClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .width(300.dp)
                    .padding(16.dp)
            ) {
                Text(
                    "Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                // Privacy Policy
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPrivacyPolicyClick() }
                        .padding(vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Privacy Policy",
                        modifier = Modifier.padding(end = 16.dp)
                    )
                    Text("Privacy Policy", style = MaterialTheme.typography.bodyLarge)
                }

                // Terms of Service
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTermsOfServiceClick() }
                        .padding(vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Terms of Service",
                        modifier = Modifier.padding(end = 16.dp)
                    )
                    Text("Terms of Service", style = MaterialTheme.typography.bodyLarge)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun IconButton(onClick: () -> Unit) {
    androidx.compose.material3.IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Settings"
        )
    }
}

@Composable
fun SelectionScreen(onHost: () -> Unit, onClient: () -> Unit, onSettingsClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Walkie Talkie", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(32.dp))
            Text(stringResource(R.string.host_instructions), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onHost) {
                Text("Host")
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(stringResource(R.string.client_instructions), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onClient) {
                Text("Client")
            }
        }
        
        // Settings button in top-right corner
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings"
            )
        }
    }
}

@Composable
fun HostScreen(ipAddress: String, clientCount: Int) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Your IP Address:")
        Text(ipAddress, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        if (clientCount == 0) {
            Text("Waiting for clients to connect...")
        } else {
            Text("$clientCount client(s) connected")
        }
    }
}

@Composable
fun ClientScreen(ipAddress: String, onConnect: (String) -> Unit) {
    var ipAddress by remember { mutableStateOf(ipAddress) }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Enter Host IP", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.client_instructions), textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
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
fun PurchaseRequiredScreen(
    productPrice: String?,
    onPurchase: (Activity) -> Unit,
    onRestorePurchases: () -> Unit,
    onDismiss: () -> Unit,
    errorMessage: String?
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var isLoading by remember { mutableStateOf(false) }
    
    // Show error message if present
    errorMessage?.let { message ->
        LaunchedEffect(message) {
            // Auto-dismiss error after 5 seconds
            delay(5000)
            onDismiss()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App logo/icon
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "App Icon",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Premium Required", style = MaterialTheme.typography.headlineLarge)
            Text("Unlock full features", style = MaterialTheme.typography.bodyLarge)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Features card
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🎙️ WalkieTalkie Premium", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Features list with icons
                    PremiumFeatureItem(icon = Icons.Default.Check, text = "Unlimited usage")
                    PremiumFeatureItem(icon = Icons.Default.Check, text = "High quality audio")
                    PremiumFeatureItem(icon = Icons.Default.Check, text = "Priority support")
                    PremiumFeatureItem(icon = Icons.Default.Check, text = "Future updates included")
                    PremiumFeatureItem(icon = Icons.Default.Check, text = "Ad-free experience")
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Pricing
                    if (productPrice != null) {
                        Text("One-time purchase:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            productPrice, 
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Loading price...", style = MaterialTheme.typography.bodySmall)
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Purchase button
                    Button(
                        onClick = { 
                            if (activity != null && productPrice != null) {
                                isLoading = true
                                onPurchase(activity)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = productPrice != null && !isLoading,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Upgrade to Premium")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Restore purchase button
                    TextButton(onClick = onRestorePurchases) {
                        Text("Restore Purchases")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Dismiss button
                    TextButton(onClick = onDismiss) {
                        Text("Not now", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
        
        // Loading overlay
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Processing purchase...", color = Color.White)
            }
        }
        
        // Show error message if present
        errorMessage?.let { message ->
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    message, 
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun PremiumFeatureItem(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun FeatureItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun ConnectedScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Connected! You can now talk.", style = MaterialTheme.typography.headlineSmall)
    }
}