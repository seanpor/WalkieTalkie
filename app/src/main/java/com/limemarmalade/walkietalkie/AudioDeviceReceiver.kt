package com.limemarmalade.walkietalkie

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager

class AudioDeviceReceiver(private val onDeviceChange: () -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            AudioManager.ACTION_HEADSET_PLUG,
            AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED -> {
                onDeviceChange()
            }
        }
    }
}
