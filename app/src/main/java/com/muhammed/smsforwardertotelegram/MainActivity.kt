package com.muhammed.smsforwardertotelegram

import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.widget.Button
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private val PERMISSION_REQUEST_CODE = 123
    private val smsReceiver = SMSReceiver()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // İzinleri kontrol et
        checkPermissions()

        // Test butonu işleyicisi
        findViewById<Button>(R.id.btnTestMessage).setOnClickListener {
            sendTestMessage()
        }
    }

    private fun sendTestMessage() {
        val testMessage = "Test Mesajı\nGönderen: Test\nTarih: ${
            SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(
            Date()
        )}\nMesaj: Bu bir test mesajıdır."
        smsReceiver.sendToTelegram(testMessage, this)
        Toast.makeText(this, "Test mesajı gönderiliyor", Toast.LENGTH_SHORT).show()
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS),
                PERMISSION_REQUEST_CODE
            )
        } else {
            // İzinler zaten verilmiş, servisi başlat
            startSmsService()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startSmsService()
            } else {
                Toast.makeText(this, "SMS izinleri olmadan uygulama çalışamaz", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startSmsService1() {
        // BroadcastReceiver'ı programatik olarak kaydet
        val intentFilter = IntentFilter("android.provider.Telephony.SMS_RECEIVED")
        val receiver = SMSReceiver()
        registerReceiver(receiver, intentFilter)

        // Kullanıcıya bilgi ver
        Toast.makeText(this, "SMS yönlendirme servisi başlatıldı", Toast.LENGTH_SHORT).show()
    }

    private fun startSmsService() {
        // Servisi başlat
        Intent(this, SMSService::class.java).also { intent ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }

        Toast.makeText(this, "SMS yönlendirme servisi başlatıldı", Toast.LENGTH_SHORT).show()
    }
}