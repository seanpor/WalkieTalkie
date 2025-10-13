package com.limemarmalade.walkietalkie

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import android.util.Log

@SuppressLint("MissingPermission")
class Audio(
    context: Context,
    private val sampleRate: Int = 16000,  // Changed from 44100 - much better for voice
    private val channelConfig: Int = AudioFormat.CHANNEL_IN_MONO,
    private val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT
) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var originalAudioMode: Int = audioManager.mode

    // Multiply by 4 for safety - prevents buffer overruns
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 4
    private lateinit var audioRecord: AudioRecord
    private lateinit var audioTrack: AudioTrack

    init {
        Log.d("Audio", "Buffer size: $bufferSize bytes")
    }

    fun start() {
        originalAudioMode = audioManager.mode
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

        val devices = audioManager.availableCommunicationDevices
        val bluetoothDevice = devices.find { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO }

        if (bluetoothDevice != null) {
            Log.d("Audio", "Bluetooth device found, setting as communication device")
            audioManager.setCommunicationDevice(bluetoothDevice)
            audioManager.startBluetoothSco()
            audioManager.isBluetoothScoOn = true
            audioManager.isSpeakerphoneOn = false
        } else {
            // Fallback to other devices if no bluetooth headset is connected
            val preferredDevice = devices.find {
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                        it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE ||
                        it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            }
            preferredDevice?.let {
                Log.d("Audio", "Setting communication device: ${it.type}")
                audioManager.setCommunicationDevice(it)
            }
        }

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,  // Changed from MIC - better for voice
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(audioFormat)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(bufferSize)
            .build()

        audioRecord.startRecording()
        audioTrack.play()

        Log.d("Audio", "AudioRecord state: ${audioRecord.recordingState}")
        Log.d("Audio", "AudioTrack state: ${audioTrack.playState}")
    }

    fun record(): ByteArray {
        val buffer = ByteArray(bufferSize)
        val bytesRead = audioRecord.read(buffer, 0, bufferSize)

        if (bytesRead > 0) {
            // Return only the bytes actually read
            return buffer.copyOf(bytesRead)
        }

        Log.w("Audio", "Failed to read audio: $bytesRead")
        return ByteArray(0)
    }

    fun play(data: ByteArray) {
        if (data.isEmpty()) return

        val written = audioTrack.write(data, 0, data.size)
        if (written < 0) {
            Log.e("Audio", "Error writing to AudioTrack: $written")
        }
    }

    fun getBufferSize(): Int = bufferSize

    fun restart() {
        stop()
        start()
    }

    fun stop() {
        if (this::audioRecord.isInitialized) {
            if (audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord.stop()
            }
            audioRecord.release()
        }
        if (this::audioTrack.isInitialized) {
            if (audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING) {
                audioTrack.stop()
            }
            audioTrack.release()
        }

        // Reset speaker mode
        audioManager.mode = originalAudioMode

        if (audioManager.isBluetoothScoOn) {
            audioManager.isBluetoothScoOn = false
            audioManager.stopBluetoothSco()
        }

        audioManager.clearCommunicationDevice()
    }
}