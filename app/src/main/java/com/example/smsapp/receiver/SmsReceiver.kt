package com.example.smsapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log
import com.example.smsapp.util.SmsUtils

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Telephony.Sms.Intents.SMS_DELIVER_ACTION == intent.action ||
            Telephony.Sms.Intents.SMS_RECEIVED_ACTION == intent.action
        ) {
            try {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                val from = messages.firstOrNull()?.displayOriginatingAddress.orEmpty()
                val body = messages.joinToString(separator = "") { it.displayMessageBody }

                Log.d("SmsReceiver", "Incoming SMS from $from: $body")
                SmsUtils.showIncomingMessageNotification(context, from, body)

                // When default, these messages are already persisted by the system.
                // Force a refresh so the UI shows them immediately.
                SmsUtils.refreshMessages(context)
            } catch (e: Exception) {
                Log.e("SmsReceiver", "Error handling incoming SMS", e)
            }
        }
    }
}
