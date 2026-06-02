package com.trace.app.proxy

import timber.log.Timber
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import kotlin.coroutines.cancellation.CancellationException

/**
 * Reads IP packets from TUN interface and forwards to local proxy.
 * Handles packet parsing and reassembly.
 */
class Tunneller(
    private val tunFd: FileDescriptor,
    private val onPacketReceived: (ByteArray) -> Unit
) {

    private var isRunning = false
    private val inputStream = FileInputStream(tunFd)
    private val outputStream = FileOutputStream(tunFd)
    private val inputChannel: FileChannel = inputStream.channel
    private val outputChannel: FileChannel = outputStream.channel

    companion object {
        private const val MAX_PACKET_SIZE = 32767 // Max IP packet size
        private const val IP_HEADER_MIN_LENGTH = 20
        private const val TCP_HEADER_MIN_LENGTH = 20
    }

    fun start() {
        if (isRunning) {
            Timber.w("Tunneller already running")
            return
        }

        isRunning = true
        Timber.i("Tunneller started")

        try {
            readPackets()
        } catch (e: CancellationException) {
            Timber.i("Tunneller cancelled")
        } catch (e: Exception) {
            Timber.e(e, "Error in tunneller")
        } finally {
            stop()
        }
    }

    private fun readPackets() {
        val buffer = ByteBuffer.allocate(MAX_PACKET_SIZE)

        while (isRunning) {
            try {
                buffer.clear()
                val bytesRead = inputChannel.read(buffer)

                if (bytesRead > 0) {
                    buffer.flip()
                    val packet = ByteArray(bytesRead)
                    buffer.get(packet)

                    processPacket(packet)
                }
            } catch (e: Exception) {
                if (isRunning) {
                    Timber.e(e, "Error reading packet")
                }
            }
        }
    }

    private fun processPacket(packet: ByteArray) {
        try {
            if (packet.size < IP_HEADER_MIN_LENGTH) {
                Timber.w("Packet too small: ${packet.size} bytes")
                return
            }

            val ipVersion = (packet[0].toInt() shr 4) and 0xF
            if (ipVersion != 4) {
                Timber.d("Ignoring non-IPv4 packet (version $ipVersion)")
                return
            }

            val protocol = packet[9].toInt() and 0xFF
            if (protocol != 6) { // TCP only
                Timber.d("Ignoring non-TCP packet (protocol $protocol)")
                return
            }

            // Extract IP header length
            val ipHeaderLength = (packet[0].toInt() and 0xF) * 4

            if (packet.size < ipHeaderLength + TCP_HEADER_MIN_LENGTH) {
                Timber.w("Packet too small for TCP: ${packet.size} bytes")
                return
            }

            // TODO: Parse TCP header and payload
            // TODO: Forward to local proxy

            onPacketReceived(packet)

        } catch (e: Exception) {
            Timber.e(e, "Error processing packet")
        }
    }

    fun writePacket(packet: ByteArray) {
        try {
            val buffer = ByteBuffer.wrap(packet)
            outputChannel.write(buffer)
        } catch (e: Exception) {
            Timber.e(e, "Error writing packet")
        }
    }

    fun stop() {
        if (!isRunning) return

        isRunning = false

        try {
            inputChannel.close()
            outputChannel.close()
            inputStream.close()
            outputStream.close()
        } catch (e: Exception) {
            Timber.e(e, "Error stopping tunneller")
        }

        Timber.i("Tunneller stopped")
    }
}
