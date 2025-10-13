package com.limemarmalade.walkietalkie

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.net.NetworkInterface
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class Screen {
    Selection,
    Host,
    Client,
    Connected
}

enum class Mode {
    HOST,
    CLIENT
}

class MainViewModel(
    application: Application,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : AndroidViewModel(application) {

    private val _screen = MutableStateFlow(Screen.Selection)
    val screen = _screen.asStateFlow()

    private val prefs = application.getSharedPreferences("walkie_talkie_prefs", Context.MODE_PRIVATE)

    private val _ipAddress = MutableStateFlow(getLastIpAddress())
    val ipAddress = _ipAddress.asStateFlow()

    private val _clientCount = MutableStateFlow(0)
    val clientCount = _clientCount.asStateFlow()

    fun onModeSelected(mode: Mode) {
        _screen.value = when (mode) {
            Mode.HOST -> {
                viewModelScope.launch(dispatcher) {
                    _ipAddress.value = getIpAddress()
                    val intent = Intent(getApplication(), WalkieTalkieService::class.java).apply {
                        action = "HOST"
                    }
                    getApplication<Application>().startService(intent)
                }
                Screen.Host
            }
            Mode.CLIENT -> Screen.Client
        }
    }

    fun onConnect(ip: String) {
        saveIpAddress(ip)
        val intent = Intent(getApplication(), WalkieTalkieService::class.java).apply {
            action = "CLIENT"
            putExtra("IP", ip)
        }
        getApplication<Application>().startService(intent)
        _screen.value = Screen.Connected
    }

    fun updateClientCount(count: Int) {
        _clientCount.value = count
    }

    private fun saveIpAddress(ip: String) {
        val editor = prefs.edit()
        editor.putString("last_ip", ip)
        editor.apply()
    }

    private fun getLastIpAddress(): String {
        return prefs.getString("last_ip", "") ?: ""
    }

    private fun getIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.isUp && !networkInterface.isLoopback &&
                    (networkInterface.displayName.contains("wlan", ignoreCase = true) ||
                            networkInterface.displayName.contains("ap", ignoreCase = true))
                ) {
                    val addresses = networkInterface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val inetAddress = addresses.nextElement()
                        if (!inetAddress.isLoopbackAddress && inetAddress.hostAddress.contains(".")) {
                            return inetAddress.hostAddress
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Log the exception or handle it as needed
        }
        return ""
    }

    public override fun onCleared() {
        super.onCleared()
        getApplication<Application>().stopService(Intent(getApplication(), WalkieTalkieService::class.java))
    }
}