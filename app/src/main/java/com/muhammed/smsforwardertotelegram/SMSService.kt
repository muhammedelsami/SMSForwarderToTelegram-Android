package com.muhammed.smsforwardertotelegram

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.telephony.SmsMessage
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SMSService : Service() {
    //private val smsReceiver = SMSReceiver()
    private val CHANNEL_ID = "SMSForwarderService"
    private val NOTIFICATION_ID = 1
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())

    private val smsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "android.provider.Telephony.SMS_RECEIVED") {
                val bundle = intent.extras
                if (bundle != null) {
                    val pdus = bundle.get("pdus") as Array<*>
                    for (i in pdus.indices) {
                        val smsMessage = SmsMessage.createFromPdu(pdus[i] as ByteArray)
                        val sender = smsMessage.originatingAddress
                        val messageBody = smsMessage.messageBody
                        val time = smsMessage.timestampMillis
                        val date = Date(time)
                        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                        val formattedDate = sdf.format(date)

                        Log.d("TAG2222", "onReceive: $messageBody")
                        // SMS mesajını hazırla
                        val message = "Yeni SMS:\nGönderen: $sender\nTarih: $formattedDate\nMesaj: $messageBody"

                        // Telegrama gönder
                        //SMSReceiver().sendToTelegram(message, context)

                        // Mesaj içeriği kontrol edilir ve uygunsa Telegrama iletme işlemi yapılır
                        // (Sadece B002 kodlu mesajlar ve token SMS'leri iletilecek)
                        if (messageBody.contains("Kullanıcı Adınız:") &&
                            messageBody.contains("Token Kodunuz:") &&
                            messageBody.contains("B002")) {
                            SMSReceiver().sendToTelegram(message, context)
                            Log.d("SMSService", "Token SMS'i iletildi")
                        }
                        else {
                            Log.d("SMSService", "onReceive: Mesaj içeriği uygun değil")
                        }
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        //startForeground(NOTIFICATION_ID, createNotification())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }
        // SMS alıcıyı kaydet
        registerReceiver(smsReceiver, IntentFilter("android.provider.Telephony.SMS_RECEIVED"))
        startLogging()
    }

    private fun startLogging() {
        serviceScope.launch {
            while (true) {
                val currentTime = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(
                    Date()
                )
                Log.d("SMSService", "Service is running at $currentTime")
                delay(5000) // Log her 5 saniyede bir
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        unregisterReceiver(smsReceiver)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SMS Forwarder Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("SMS Forwarder")
        .setContentText("SMS iletme servisi çalışıyor")
        .setSmallIcon(R.drawable.ic_launcher_background)
        .build()
}