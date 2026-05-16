package com.example.byedpi

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ProxyInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.InputStream
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

class DpiBypassService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var proxyServer: ServerSocket? = null
    private var isRunning = false

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "ByeDPI_Channel"
        private const val PROXY_PORT = 8080
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            isRunning = true
            createNotificationChannel()
            startForeground(NOTIFICATION_ID, createNotification())
            startProxyServer()
            startVpn()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        cleanup()
        stopForeground(true)
        super.onDestroy()
    }

    override fun onRevoke() {
        isRunning = false
        cleanup()
        super.onRevoke()
    }

    private fun cleanup() {
        try { proxyServer?.close() } catch (e: Exception) {}
        try { vpnInterface?.close() } catch (e: Exception) {}
        vpnInterface = null
        proxyServer = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ByeDPI Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ByeDPI is active")
            .setContentText("Protecting your privacy...")
            .setSmallIcon(R.drawable.ic_vpn_stat)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun startVpn() {
        try {
            val builder = Builder()
                .setSession("ByeDPI-VPN")
                .addAddress("10.0.0.2", 32)
                // addRoute("0.0.0.0", 0) kaldırıldı: Sadece HTTP proxy kullanılacak, tüm trafiğin kaybolması engellendi.
                
            // Kendi uygulamamızı proxy döngüsünden çıkarıyoruz
            try {
                builder.addDisallowedApplication(packageName)
            } catch (e: Exception) {
                Log.e("ByeDPI", "Error disallowing app", e)
            }

            // Android 10+ handles HTTP Proxy via VpnService.Builder
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setHttpProxy(ProxyInfo.buildDirectProxy("127.0.0.1", PROXY_PORT))
            } else {
                Log.w("ByeDPI", "HTTP Proxy via VpnService requires Android 10+")
            }
            
            vpnInterface = builder.establish()
            Log.d("ByeDPI", "VPN Interface established")
        } catch (e: Exception) {
            Log.e("ByeDPI", "VPN Service error", e)
            stopSelf()
        }
    }

    private fun startProxyServer() {
        thread(name = "ProxyServerThread") {
            try {
                proxyServer = ServerSocket(PROXY_PORT)
                Log.d("ByeDPI", "Local proxy listening on $PROXY_PORT")

                while (isRunning) {
                    val clientSocket = proxyServer?.accept() ?: break
                    thread { handleClient(clientSocket) }
                }
            } catch (e: Exception) {
                if (isRunning) Log.e("ByeDPI", "Proxy Server error", e)
            }
        }
    }

    private fun readLine(input: InputStream): String {
        val sb = StringBuilder()
        while (isRunning) {
            val c = input.read()
            if (c == -1) break
            if (c == '\n'.code) {
                if (sb.isNotEmpty() && sb[sb.length - 1] == '\r') {
                    sb.deleteCharAt(sb.length - 1)
                }
                break
            }
            sb.append(c.toChar())
        }
        return sb.toString()
    }

    private fun handleClient(clientSocket: Socket) {
        clientSocket.use { client ->
            try {
                val clientInput = client.getInputStream()
                val clientOutput = client.getOutputStream()
                
                val requestLine = readLine(clientInput)
                if (requestLine.isEmpty()) return
                
                val parts = requestLine.split(" ")
                if (parts.size < 2) return
                
                val method = parts[0]
                val url = parts[1]
                
                if (method == "CONNECT") {
                    val hostPort = url.split(":")
                    val targetHost = hostPort[0]
                    val targetPort = hostPort.getOrNull(1)?.toInt() ?: 443
                    
                    // Skip remaining headers
                    while (readLine(clientInput).isNotEmpty()) { }
                    
                    val targetSocket = Socket(targetHost, targetPort)
                    targetSocket.use { target ->
                        protect(target)
                        target.tcpNoDelay = true
                        client.tcpNoDelay = true
                        
                        clientOutput.write("HTTP/1.1 200 Connection Established\r\n\r\n".toByteArray())
                        clientOutput.flush()
                        
                        pipeData(client, target)
                    }
                } else {
                    // Simple HTTP proxying could be added here
                    Log.d("ByeDPI", "Method $method not supported yet")
                }
            } catch (e: Exception) {
                Log.e("ByeDPI", "Client handling error", e)
            }
        }
    }

    private fun pipeData(client: Socket, target: Socket) {
        val clientIn = client.getInputStream()
        val targetOut = target.getOutputStream()
        val targetIn = target.getInputStream()
        val clientOut = client.getOutputStream()

        val t1 = thread {
            try {
                val buffer = ByteArray(16384)
                var isFirstPacket = true
                
                while (isRunning) {
                    val bytesRead = clientIn.read(buffer)
                    if (bytesRead == -1) break
                    
                    if (isFirstPacket && bytesRead > 5 && buffer[0] == 0x16.toByte() && buffer[1] == 0x03.toByte()) {
                        // TCP Split for TLS ClientHello
                        try {
                            targetOut.write(buffer, 0, 3)
                            targetOut.flush()
                            Thread.sleep(15)
                            targetOut.write(buffer, 3, bytesRead - 3)
                            targetOut.flush()
                        } catch (e: Exception) { break }
                        isFirstPacket = false
                    } else {
                        targetOut.write(buffer, 0, bytesRead)
                        targetOut.flush()
                    }
                }
            } catch (e: Exception) {
            } finally {
                try { target.close() } catch (e: Exception) {}
                try { client.close() } catch (e: Exception) {}
            }
        }

        val t2 = thread {
            try {
                val buffer = ByteArray(16384)
                while (isRunning) {
                    val bytesRead = targetIn.read(buffer)
                    if (bytesRead == -1) break
                    clientOut.write(buffer, 0, bytesRead)
                    clientOut.flush()
                }
            } catch (e: Exception) {
            } finally {
                try { client.close() } catch (e: Exception) {}
                try { target.close() } catch (e: Exception) {}
            }
        }

        try {
            t1.join()
            t2.join()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }
}
