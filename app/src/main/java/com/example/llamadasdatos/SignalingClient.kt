package com.example.llamadasdatos

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Habla con el servidor de señalización (index.js) por WebSocket.
 * Solo intercambia mensajes de control: registro, oferta/respuesta SDP,
 * candidatos ICE y colgar. El audio no pasa por aquí.
 */
class SignalingClient(
    private val serverUrl: String, // ej: "wss://tu-servidor.onrender.com"
    private val myPhone: String,
    private val listener: Listener
) {
    interface Listener {
        fun onRegistered()
        fun onIncomingCall(fromPhone: String, sdp: String)
        fun onCallAnswered(fromPhone: String, sdp: String)
        fun onRemoteIceCandidate(fromPhone: String, sdpMid: String, sdpMLineIndex: Int, candidate: String)
        fun onRejected(fromPhone: String)
        fun onHangup(fromPhone: String)
        fun onUnavailable(phone: String)
        fun onConnectionError(message: String)
    }

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS) // mantiene viva la conexión en datos móviles
        .build()

    fun connect() {
        val request = Request.Builder().url(serverUrl).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                send(JSONObject().apply {
                    put("type", "register")
                    put("phone", myPhone)
                })
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("SignalingClient", "Fallo de conexión: ${t.message}")
                listener.onConnectionError(t.message ?: "error desconocido")
            }
        })
    }

    private fun handleMessage(text: String) {
        val json = JSONObject(text)
        when (json.optString("type")) {
            "registered" -> listener.onRegistered()

            "incoming_call" -> listener.onIncomingCall(
                json.getString("from"),
                json.getString("sdp")
            )

            "call_answered" -> listener.onCallAnswered(
                json.getString("from"),
                json.getString("sdp")
            )

            "ice_candidate" -> {
                val candidateObj = json.getJSONObject("candidate")
                listener.onRemoteIceCandidate(
                    json.getString("from"),
                    candidateObj.getString("sdpMid"),
                    candidateObj.getInt("sdpMLineIndex"),
                    candidateObj.getString("candidate")
                )
            }

            "rejected" -> listener.onRejected(json.getString("from"))
            "hangup" -> listener.onHangup(json.getString("from"))
            "unavailable" -> listener.onUnavailable(json.getString("to"))
        }
    }

    fun sendCallOffer(toPhone: String, sdp: String) {
        send(JSONObject().apply {
            put("type", "call")
            put("to", toPhone)
            put("sdp", sdp)
        })
    }

    fun sendAnswer(toPhone: String, sdp: String) {
        send(JSONObject().apply {
            put("type", "answer")
            put("to", toPhone)
            put("sdp", sdp)
        })
    }

    fun sendIceCandidate(toPhone: String, sdpMid: String, sdpMLineIndex: Int, candidate: String) {
        send(JSONObject().apply {
            put("type", "ice_candidate")
            put("to", toPhone)
            put("candidate", JSONObject().apply {
                put("sdpMid", sdpMid)
                put("sdpMLineIndex", sdpMLineIndex)
                put("candidate", candidate)
            })
        })
    }

    fun sendReject(toPhone: String) {
        send(JSONObject().apply {
            put("type", "reject")
            put("to", toPhone)
        })
    }

    fun sendHangup(toPhone: String) {
        send(JSONObject().apply {
            put("type", "hangup")
            put("to", toPhone)
        })
    }

    private fun send(json: JSONObject) {
        webSocket?.send(json.toString())
    }

    fun disconnect() {
        webSocket?.close(1000, "cierre normal")
        webSocket = null
    }
}
