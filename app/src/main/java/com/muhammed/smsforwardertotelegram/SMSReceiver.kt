package com.muhammed.smsforwardertotelegram

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SMSReceiver : BroadcastReceiver() {
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

                    // SMS bilgilerini hazırlama
                    val message = "Yeni SMS:\nGönderen: $sender\nTarih: $formattedDate\nMesaj: $messageBody"

                    // Telegram'a gönder
                    sendToTelegram(message, context)
                }
            }
        }
    }

    fun sendToTelegram(message: String, context: Context) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                // Telegram Bot API kullanarak mesaj gönderme
                val botToken = "" // Telegram botunuzun token'ı
                val chatId = "" // Hedef kanal veya grup ID'si

                val encodedMessage = URLEncoder.encode(message, "UTF-8")
                val url = "https://api.telegram.org/bot$botToken/sendMessage?chat_id=$chatId&text=$encodedMessage"

                val client = OkHttpClient()
                val request = Request.Builder()
                    .url(url)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("TelegramSender", "Mesaj gönderilemedi: ${response.message}")
                    }
                    else {
                        Log.i("TelegramSender", "Mesaj gönderildi: $message")
                    }
                }
            } catch (e: Exception) {
                Log.e("TelegramSender", "Hata: ${e.message}")
            }
        }
    }
}