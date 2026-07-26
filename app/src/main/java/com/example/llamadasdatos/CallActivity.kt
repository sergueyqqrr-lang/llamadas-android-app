package com.example.llamadasdatos

import android.media.AudioManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.llamadasdatos.databinding.ActivityCallBinding
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

class CallActivity : AppCompatActivity(), SignalingClient.Listener {

    private lateinit var binding: ActivityCallBinding
    private lateinit var webRtcClient: WebRtcClient
    private lateinit var otherPhone: String
    private var isMuted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // El audio de la llamada sale por el auricular/altavoz, no por el "modo llamada" de la red celular
        val audioManager = getSystemService(AudioManager::class.java)
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = true

        otherPhone = intent.getStringExtra("phone") ?: run { finish(); return }
        val mode = intent.getStringExtra("mode")

        binding.tvCallPhone.text = otherPhone
        binding.tvCallStatus.text = if (mode == "OUTGOING") "Llamando..." else "Conectando..."

        // Esta activity pasa a ser quien recibe los eventos de señalización
        CallManager.uiListener = this

        webRtcClient = WebRtcClient(applicationContext, object : WebRtcClient.Callback {
            override fun onLocalIceCandidate(candidate: IceCandidate) {
                CallManager.signalingClient?.sendIceCandidate(
                    otherPhone, candidate.sdpMid, candidate.sdpMLineIndex, candidate.sdp
                )
            }

            override fun onLocalSdp(sdp: SessionDescription) {
                if (sdp.type == SessionDescription.Type.OFFER) {
                    CallManager.signalingClient?.sendCallOffer(otherPhone, sdp.description)
                } else {
                    CallManager.signalingClient?.sendAnswer(otherPhone, sdp.description)
                }
            }

            override fun onConnected() {
                runOnUiThread { binding.tvCallStatus.text = "En llamada" }
            }

            override fun onDisconnected() {
                runOnUiThread { finish() }
            }
        })

        if (mode == "OUTGOING") {
            webRtcClient.startCall()
        } else {
            val sdp = CallManager.pendingIncomingCallSdp
            if (sdp != null) {
                webRtcClient.acceptCall(sdp)
            }
            CallManager.pendingIncomingCallFrom = null
            CallManager.pendingIncomingCallSdp = null
        }

        binding.btnHangup.setOnClickListener {
            CallManager.signalingClient?.sendHangup(otherPhone)
            webRtcClient.endCall()
            finish()
        }

        binding.btnMute.setOnClickListener {
            isMuted = !isMuted
            webRtcClient.setMuted(isMuted)
            binding.btnMute.text = if (isMuted) "Activar mic" else "Silenciar"
        }
    }

    // --- Eventos de señalización relevantes durante la llamada ---

    override fun onCallAnswered(fromPhone: String, sdp: String) {
        if (fromPhone == otherPhone) {
            runOnUiThread { webRtcClient.onRemoteAnswer(sdp) }
        }
    }

    override fun onRemoteIceCandidate(fromPhone: String, sdpMid: String, sdpMLineIndex: Int, candidate: String) {
        if (fromPhone == otherPhone) {
            webRtcClient.addRemoteIceCandidate(sdpMid, sdpMLineIndex, candidate)
        }
    }

    override fun onRejected(fromPhone: String) {
        if (fromPhone == otherPhone) {
            runOnUiThread {
                binding.tvCallStatus.text = "Llamada rechazada"
                finish()
            }
        }
    }

    override fun onHangup(fromPhone: String) {
        if (fromPhone == otherPhone) {
            webRtcClient.endCall()
            runOnUiThread { finish() }
        }
    }

    override fun onUnavailable(phone: String) {
        runOnUiThread {
            binding.tvCallStatus.text = "No disponible"
            finish()
        }
    }

    override fun onRegistered() {}
    override fun onIncomingCall(fromPhone: String, sdp: String) {}
    override fun onConnectionError(message: String) {
        runOnUiThread { binding.tvCallStatus.text = "Error de conexión" }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (CallManager.uiListener == this) {
            CallManager.uiListener = null
        }
        val audioManager = getSystemService(AudioManager::class.java)
        audioManager.mode = AudioManager.MODE_NORMAL
    }
}
