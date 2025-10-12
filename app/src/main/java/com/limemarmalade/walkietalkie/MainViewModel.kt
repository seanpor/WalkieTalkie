package com.limemarmalade.walkietalkie

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _screen = MutableStateFlow(Screen.Selection)
    val screen = _screen.asStateFlow()

    private val _ipAddress = MutableStateFlow("")
    val ipAddress = _ipAddress.asStateFlow()

    fun onModeSelected(mode: Mode) {
        _screen.value = when (mode) {
            Mode.HOST -> {
                viewModelScope.launch {
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
        val intent = Intent(getApplication(), WalkieTalkieService::class.java).apply {
            action = "CLIENT"
            putExtra("IP", ip)
        }
        getApplication<Application>().startService(intent)
        _screen.value = Screen.Connected
    }

    private fun getIpAddress(): String {
        val connectivityManager =
            getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val linkProperties =
            connectivityManager.getLinkProperties(connectivityManager.activeNetwork)
        linkProperties?.linkAddresses?.forEach { linkAddress ->
            if (linkAddress.address.hostAddress?.contains(".") == true) {
                return linkAddress.address.hostAddress!!
            }
        }
        return ""
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().stopService(Intent(getApplication(), WalkieTalkieService::class.java))
    }
}
