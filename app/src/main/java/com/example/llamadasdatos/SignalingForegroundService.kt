package com.example.llamadasdatos

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Corre en primer plano para que el teléfono no mate la conexión de
 * señalización mientras la app está en segundo plano — así se pueden
 * recibir llamadas entrantes en cualquier momento, igual que con la app
 * de teléfono normal.
 */
class SignalingForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "signaling_channel"
        const val INCOMING_CHANNEL_ID = "incoming_call_channel"
        const val NOTIF_ID = 1
        const val INCOMING_NOTIF_ID = 2
    }

    private val listener = object : SignalingClient.Listener {
        override fun onRegistered() {
            CallManager.uiListener?.onRegistered()
        }

        override fun onIncomingCall(fromPhone: String, sdp: String) {
            CallManager.pendingIncomingCallFrom = fromPhone
            CallManager.pendingIncomingCallSdp = sdp
            showIncomingCallNotification(fromPhone)
            CallManager.uiListener?.onIncomingCall(fromPhone, sdp)
        }

        override fun onCallAnswered(fromPhone: String, sdp: String) {
            CallManager.uiListener?.onCallAnswered(fromPhone, sdp)
        }

        override fun onRemoteIceCandidate(fromPhone: String, sdpMid: String, sdpMLineIndex: Int, candidate: String) {
            CallManager.uiListener?.onRemoteIceCandidate(fromPhone, sdpMid, sdpMLineIndex, candidate)
        }

        override fun onRejected(fromPhone: String) {
            CallManager.uiListener?.onRejected(fromPhone)
        }

        override fun onHangup(fromPhone: String) {
            CallManager.uiListener?.onHangup(fromPhone)
        }

        override fun onUnavailable(phone: String) {
            CallManager.uiListener?.onUnavailable(phone)
        }

        override fun onConnectionError(message: String) {
            CallManager.uiListener?.onConnectionError(message)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannels()
        startForeground(NOTIF_ID, buildPersistentNotification())
        CallManager.ensureConnected(applicationContext, listener)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Conexión activa", NotificationManager.IMPORTANCE_LOW)
            )
            manager.createNotificationChannel(
                NotificationChannel(
                    INCOMING_CHANNEL_ID, "Llamadas entrantes", NotificationManager.IMPORTANCE_HIGH
                )
            )
        }
    }

    private fun buildPersistentNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Llamadas por Datos")
            .setContentText("Listo para recibir llamadas")
            .setSmallIcon(android.R.drawable.sym_call_incoming)
            .setOngoing(true)
            .build()
    }

    private fun showIncomingCallNotification(fromPhone: String) {
        val fullScreenIntent = Intent(this, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("phone", fromPhone)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 0, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, INCOMING_CHANNEL_ID)
            .setContentTitle("Llamada entrante")
            .setContentText(fromPhone)
            .setSmallIcon(android.R.drawable.sym_call_incoming)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(INCOMING_NOTIF_ID, notification)
    }
}
