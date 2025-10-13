package com.limemarmalade.walkietalkie

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.InetSocketAddress

class WalkieTalkieService : Service() {

    private val CHANNEL_ID = "WalkieTalkieChannel"
    private val NOTIFICATION_ID = 1
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var server: Server? = null
    private var client: Client? = null
    private var audio: Audio? = null

    inner class WalkieTalkieBinder : Binder() {
        fun getService(): WalkieTalkieService = this@WalkieTalkieService
    }

    private val binder = WalkieTalkieBinder()

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    fun getClientCount(): Int {
        return server?.getClientCount() ?: 0
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification("Walkie Talkie is active")

        // CRITICAL: Must specify service type for Android 14+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("Service", "No RECORD_AUDIO permission")
            stopSelf()
            return START_NOT_STICKY
        }

        audio = Audio(this).apply { start() }

        when (intent?.action) {
            "HOST" -> {
                Log.d("Service", "Starting as HOST")
                serviceScope.launch {
                    server = Server(PORT)

                    // Coroutine for receiving client audio and broadcasting to other clients
                    launch {
                        while (isActive) {
                            val receivedPacket = server!!.receive()
                            receivedPacket?.let {
                                // CRITICAL: Host PLAYS the client audio locally
                                audio!!.play(it.data)

                                // Also broadcast to OTHER clients (not back to sender)
                                server!!.broadcast(it.data, it.sender)
                            }
                        }
                    }

                    // Coroutine for recording and broadcasting host audio
                    launch {
                        delay(100)
                        var packetCount = 0
                        while (isActive) {
                            try {
                                val data = audio!!.record()
                                if (data.isNotEmpty()) {
                                    // Broadcast host's own voice to all clients
                                    server!!.broadcast(data)

                                    if (packetCount++ % 100 == 0) {
                                        Log.d("Service", "Host sent packet ${data.size} bytes to ${server!!.getClientCount()} clients")
                                    }
                                }
                                delay(10)
                            } catch (e: Exception) {
                                Log.e("Service", "Error in host recording: ${e.message}")
                            }
                        }
                    }
                }
            }
            "CLIENT" -> {
                val ip = intent.getStringExtra("IP")
                if (ip != null) {
                    Log.d("Service", "Starting as CLIENT connecting to $ip")
                    serviceScope.launch {
                        client = Client(ip, PORT)

                        // Coroutine for recording and sending audio
                        launch {
                            // Small delay to ensure audio is ready
                            delay(100)
                            while (isActive) {
                                try {
                                    val data = audio!!.record()
                                    if (data.isNotEmpty()) {
                                        client!!.send(data)
                                    }
                                    // Small delay to prevent CPU overload
                                    delay(10)
                                } catch (e: Exception) {
                                    Log.e("Service", "Error in client recording: ${e.message}")
                                }
                            }
                        }

                        // Coroutine for receiving and playing audio
                        launch {
                            while (isActive) {
                                try {
                                    val receivedData = client!!.receive()
                                    if (receivedData.isNotEmpty()) {
                                        audio!!.play(receivedData)
                                    }
                                } catch (e: Exception) {
                                    Log.e("Service", "Error in client receiving: ${e.message}")
                                }
                            }
                        }
                    }
                } else {
                    Log.e("Service", "No IP provided for CLIENT mode")
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("Service", "Service destroying")
        serviceScope.cancel()
        server?.stop()
        client?.stop()
        audio?.stop()
    }

    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Walkie Talkie")
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Walkie Talkie Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }
}