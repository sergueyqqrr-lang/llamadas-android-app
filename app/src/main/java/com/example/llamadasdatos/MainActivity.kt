package com.example.llamadasdatos

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.llamadasdatos.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val requiredPermissions = mutableListOf(
        Manifest.permission.RECORD_AUDIO
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestNeededPermissions()

        val savedPhone = CallManager.getMyPhone(this)
        if (savedPhone != null) {
            binding.etMyPhone.setText(savedPhone)
            binding.tvStatus.text = "Registrado como $savedPhone"
            startSignalingService()
        }

        binding.btnRegister.setOnClickListener {
            val phone = binding.etMyPhone.text.toString().trim()
            if (phone.isNotEmpty()) {
                CallManager.setMyPhone(this, phone)
                binding.tvStatus.text = "Registrado como $phone"
                startSignalingService()
            }
        }

        binding.btnCall.setOnClickListener {
            val target = binding.etTargetPhone.text.toString().trim()
            if (target.isNotEmpty()) {
                val intent = Intent(this, CallActivity::class.java).apply {
                    putExtra("mode", "OUTGOING")
                    putExtra("phone", target)
                }
                startActivity(intent)
            }
        }
    }

    private fun requestNeededPermissions() {
        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 100)
        }
    }

    private fun startSignalingService() {
        val intent = Intent(this, SignalingForegroundService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }
}
