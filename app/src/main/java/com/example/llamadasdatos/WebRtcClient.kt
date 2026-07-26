package com.example.llamadasdatos

import android.content.Context
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

/**
 * Encapsula todo lo de WebRTC: crear la conexión, generar oferta/respuesta
 * SDP, agregar el audio del micrófono y reproducir el audio remoto.
 *
 * Usa servidores STUN públicos gratis. Para el TURN, cambia las URLs de
 * ejemplo por las de tu proveedor gratuito (ver TURN_SETUP.md).
 */
class WebRtcClient(
    context: Context,
    private val callback: Callback
) {
    interface Callback {
        fun onLocalIceCandidate(candidate: IceCandidate)
        fun onLocalSdp(sdp: SessionDescription)
        fun onConnected()
        fun onDisconnected()
    }

    private val eglBase: EglBase = EglBase.create()

    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        // --- Reemplaza esto por tu TURN gratuito (Open Relay / ExpressTURN) ---
        PeerConnection.IceServer.builder("turn:openrelay.metered.ca:80")
            .setUsername("openrelayproject")
            .setPassword("openrelayproject")
            .createIceServer()
    )

    private val factory: PeerConnectionFactory
    private var peerConnection: PeerConnection? = null
    private var localAudioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null

    init {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .createInitializationOptions()
        )
        factory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .createPeerConnectionFactory()
    }

    private fun createPeerConnection() {
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }

        peerConnection = factory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                callback.onLocalIceCandidate(candidate)
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                when (newState) {
                    PeerConnection.PeerConnectionState.CONNECTED -> callback.onConnected()
                    PeerConnection.PeerConnectionState.DISCONNECTED,
                    PeerConnection.PeerConnectionState.FAILED,
                    PeerConnection.PeerConnectionState.CLOSED -> callback.onDisconnected()
                    else -> {}
                }
            }

            override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {}
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
            override fun onAddStream(p0: MediaStream?) {}
            override fun onRemoveStream(p0: MediaStream?) {}
            override fun onDataChannel(p0: org.webrtc.DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(p0: org.webrtc.RtpReceiver?, p1: Array<out MediaStream>?) {}
            override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
        })

        // Agregar audio del micrófono
        val audioConstraints = MediaConstraints()
        localAudioSource = factory.createAudioSource(audioConstraints)
        localAudioTrack = factory.createAudioTrack("AUDIO_TRACK", localAudioSource)
        peerConnection?.addTrack(localAudioTrack, listOf("STREAM"))
    }

    /** Quien llama: crea la conexión y genera la oferta SDP */
    fun startCall() {
        createPeerConnection()
        val constraints = MediaConstraints()
        peerConnection?.createOffer(object : SdpObserver by NoopSdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                peerConnection?.setLocalDescription(NoopSdpObserver, sdp)
                callback.onLocalSdp(sdp)
            }
        }, constraints)
    }

    /** Quien recibe: procesa la oferta y genera la respuesta SDP */
    fun acceptCall(remoteSdp: String) {
        createPeerConnection()
        peerConnection?.setRemoteDescription(
            NoopSdpObserver,
            SessionDescription(SessionDescription.Type.OFFER, remoteSdp)
        )
        val constraints = MediaConstraints()
        peerConnection?.createAnswer(object : SdpObserver by NoopSdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                peerConnection?.setLocalDescription(NoopSdpObserver, sdp)
                callback.onLocalSdp(sdp)
            }
        }, constraints)
    }

    /** Quien llamó, al recibir la respuesta del otro lado */
    fun onRemoteAnswer(remoteSdp: String) {
        peerConnection?.setRemoteDescription(
            NoopSdpObserver,
            SessionDescription(SessionDescription.Type.ANSWER, remoteSdp)
        )
    }

    fun addRemoteIceCandidate(sdpMid: String, sdpMLineIndex: Int, candidate: String) {
        peerConnection?.addIceCandidate(IceCandidate(sdpMid, sdpMLineIndex, candidate))
    }

    fun setMuted(muted: Boolean) {
        localAudioTrack?.setEnabled(!muted)
    }

    fun endCall() {
        peerConnection?.close()
        peerConnection = null
        localAudioSource?.dispose()
    }
}

/** SdpObserver con implementaciones vacías, para no repetir código */
object NoopSdpObserver : SdpObserver {
    override fun onCreateSuccess(p0: SessionDescription?) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(p0: String?) {}
    override fun onSetFailure(p0: String?) {}
}
