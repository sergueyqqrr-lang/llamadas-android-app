package com.example.llamadasdatos

import android.content.Context
import android.content.SharedPreferences

/**
 * Singleton que mantiene UNA sola conexión de señalización viva mientras la
 * app está abierta (la usa tanto el servicio en segundo plano como las
 * pantallas de llamada), y guarda el estado de la llamada entrante mientras
 * el usuario decide si contestar o no.
 */
object CallManager {

    private const val PREFS = "llamadas_prefs"
    private const val KEY_MY_PHONE = "my_phone"

    // Cambia esto por la URL real de tu servidor desplegado (ver signaling-server/README.md)
    const val SIGNALING_SERVER_URL = "wss://llamadas-signaling-server-production.up.railway.app"

    var signalingClient: SignalingClient? = null
        private set

    // Datos de la llamada entrante mientras se decide aceptar/rechazar
    var pendingIncomingCallFrom: String? = null
    var pendingIncomingCallSdp: String? = null

    // Escucha actualmente activa desde una pantalla (Call/IncomingCall)
    var uiListener: SignalingClient.Listener? = null

    fun getMyPhone(context: Context): String? {
        return prefs(context).getString(KEY_MY_PHONE, null)
    }

    fun setMyPhone(context: Context, phone: String) {
        prefs(context).edit().putString(KEY_MY_PHONE, phone).apply()
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Se llama una sola vez, típicamente desde el servicio en segundo plano */
    fun ensureConnected(context: Context, coreListener: SignalingClient.Listener) {
        if (signalingClient != null) return
        val myPhone = getMyPhone(context) ?: return
        signalingClient = SignalingClient(SIGNALING_SERVER_URL, myPhone, coreListener)
        signalingClient?.connect()
    }

    fun disconnect() {
        signalingClient?.disconnect()
        signalingClient = null
    }
}
