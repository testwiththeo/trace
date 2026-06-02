package com.trace.app.proxy

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.trace.app.R
import com.trace.app.data.repository.TrafficRepositoryImpl
import com.trace.app.engine.BlocklistFilter
import com.trace.app.engine.DelayInjector
import com.trace.app.engine.MockEngine
import com.trace.app.engine.TrafficCapturer
import com.trace.app.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * VPN Service for intercepting network traffic.
 * Creates a TUN interface and routes all traffic through local proxy.
 */
@AndroidEntryPoint
class TraceVpnService : VpnService() {

    @Inject
    lateinit var proxyServer: LocalProxyServer

    private var vpnInterface: ParcelFileDescriptor? = null
    private var tunneller: Tunneller? = null
    private var isRunning = false

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val VPN_ADDRESS = "10.0.0.2"
        private const val VPN_ROUTE = "0.0.0.0"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "trace_vpn_channel"
    }

    override fun onCreate() {
        super.onCreate()
        Timber.d("TraceVpnService created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START" -> startVpn()
            "STOP" -> stopVpn()
        }
        return START_STICKY
    }

    private fun startVpn() {
        if (isRunning) {
            Timber.w("VPN already running")
            return
        }

        try {
            // Build VPN interface
            val builder = Builder()
                .setSession("Trace VPN")
                .addAddress(VPN_ADDRESS, 24)
                .addRoute(VPN_ROUTE, 0)
                .addDnsServer("8.8.8.8")
                .addDnsServer("1.1.1.1")

            vpnInterface = builder.establish()

            if (vpnInterface == null) {
                Timber.e("Failed to establish VPN interface")
                return
            }

            isRunning = true
            startForeground(NOTIFICATION_ID, createNotification())

            Timber.i("VPN started successfully")

            // Start local proxy server
            startProxyServer()

            // Start packet reading thread
            startTunneller()

        } catch (e: Exception) {
            Timber.e(e, "Failed to start VPN")
            stopSelf()
        }
    }

    private fun startProxyServer() {
        serviceScope.launch {
            try {
                val port = proxyServer.start()
                Timber.i("Proxy server started on port $port")
            } catch (e: Exception) {
                Timber.e(e, "Failed to start proxy server")
            }
        }
    }

    private fun startTunneller() {
        serviceScope.launch {
            try {
                val fd = vpnInterface?.fileDescriptor ?: return@launch
                tunneller = Tunneller(fd) { packet ->
                    handlePacket(packet)
                }
                tunneller?.start()
                Timber.i("Tunneller started")
            } catch (e: Exception) {
                Timber.e(e, "Failed to start tunneller")
            }
        }
    }

    private fun handlePacket(packet: ByteArray) {
        // Packet received from TUN interface
        // TODO: Forward to proxy server
        // This would require parsing TCP/IP packets and reconstructing HTTP requests
        // For now, we rely on apps connecting directly to the proxy via VPN routing
        Timber.d("Received packet: ${packet.size} bytes")
    }

    private fun stopVpn() {
        Timber.i("Stopping VPN")

        isRunning = false

        try {
            tunneller?.stop()
            tunneller = null

            proxyServer.stop()

            vpnInterface?.close()
            vpnInterface = null

            serviceScope.cancel()
        } catch (e: Exception) {
            Timber.e(e, "Error closing VPN interface")
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVpn()
        Timber.d("TraceVpnService destroyed")
    }

    override fun onRevoke() {
        super.onRevoke()
        Timber.w("VPN permission revoked by system")
        stopVpn()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Trace VPN Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Traffic interception service"
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Trace Active")
            .setContentText("Capturing network traffic")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
