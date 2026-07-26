package com.example.llamadasdatos

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.llamadasdatos.databinding.ActivityIncomingCallBinding

class IncomingCallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIncomingCallBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIncomingCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val fromPhone = intent.getStringExtra("phone")
            ?: CallManager.pendingIncomingCallFrom
            ?: run { finish(); return }

        binding.tvIncomingPhone.text = fromPhone

        binding.btnAccept.setOnClickListener {
            val callIntent = Intent(this, CallActivity::class.java).apply {
                putExtra("mode", "INCOMING")
                putExtra("phone", fromPhone)
            }
            startActivity(callIntent)
            finish()
        }

        binding.btnReject.setOnClickListener {
            CallManager.signalingClient?.sendReject(fromPhone)
            CallManager.pendingIncomingCallFrom = null
            CallManager.pendingIncomingCallSdp = null
            finish()
        }
    }
}
