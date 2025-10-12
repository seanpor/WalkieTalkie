package com.limemarmalade.walkietalkie

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.SocketException

class Server(private val port: Int = PORT) {

    private val socket = DatagramSocket(port)
    private val clients = mutableSetOf<InetSocketAddress>()
    private val maxPacketSize = 8192

    init {
        Log.d("Server", "Server started on port $port")
    }

    data class ReceivedPacket(val data: ByteArray, val sender: InetSocketAddress)

    suspend fun receive(): ReceivedPacket? {
        return withContext(Dispatchers.IO) {
            try {
                val buffer = ByteArray(maxPacketSize)
                val packet = DatagramPacket(buffer, buffer.size)
                socket.receive(packet)

                val clientAddr = InetSocketAddress(packet.address, packet.port)
                if (clients.add(clientAddr)) {
                    Log.d("Server", "New client connected: $clientAddr (Total: ${clients.size})")
                }

                ReceivedPacket(
                    packet.data.copyOf(packet.length),
                    clientAddr
                )
            } catch (e: SocketException) {
                Log.d("Server", "Socket closed")
                null
            } catch (e: IOException) {
                Log.e("Server", "IOException receiving: ${e.message}")
                null
            }
        }
    }

    suspend fun broadcast(data: ByteArray, sender: InetSocketAddress? = null) {
        withContext(Dispatchers.IO) {
            clients.forEach { client ->
                if (client != sender) {
                    try {
                        val sendPacket = DatagramPacket(
                            data,
                            data.size,
                            client.address,
                            client.port
                        )
                        socket.send(sendPacket)
                    } catch (e: SocketException) {
                        Log.e("Server", "SocketException broadcasting to $client: ${e.message}")
                    } catch (e: IOException) {
                        Log.e("Server", "IOException broadcasting to $client: ${e.message}")
                    }
                }
            }
        }
    }

    fun getClientCount(): Int = clients.size

    fun stop() {
        if (!socket.isClosed) {
            Log.d("Server", "Stopping server")
            socket.close()
        }
    }
}