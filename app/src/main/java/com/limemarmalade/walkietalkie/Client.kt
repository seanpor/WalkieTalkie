package com.limemarmalade.walkietalkie

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException

class Client(private val host: String, private val port: Int = PORT) {

    private val socket = DatagramSocket()
    private val maxPacketSize = 8192  // Large enough for audio buffers

    init {
        socket.connect(InetAddress.getByName(host), port)
        Log.d("Client", "Connected to $host:$port")
    }

    suspend fun send(data: ByteArray) {
        withContext(Dispatchers.IO) {
            try {
                val packet = DatagramPacket(data, data.size)
                socket.send(packet)
            } catch (e: SocketException) {
                Log.e("Client", "SocketException sending: ${e.message}")
            } catch (e: IOException) {
                Log.e("Client", "IOException sending: ${e.message}")
            }
        }
    }

    suspend fun receive(): ByteArray {
        return withContext(Dispatchers.IO) {
            try {
                val buffer = ByteArray(maxPacketSize)
                val packet = DatagramPacket(buffer, buffer.size)
                socket.receive(packet)

                Log.d("Client", "Received ${packet.length} bytes")

                // Return only the actual data received, not the full buffer
                packet.data.copyOf(packet.length)
            } catch (e: SocketException) {
                Log.e("Client", "SocketException receiving: ${e.message}")
                ByteArray(0)
            } catch (e: IOException) {
                Log.e("Client", "IOException receiving: ${e.message}")
                ByteArray(0)
            }
        }
    }

    fun stop() {
        if (!socket.isClosed) {
            socket.close()
        }
    }
}