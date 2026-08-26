package com.example.smsapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.smsapp.util.SmsUtils

class ScheduledSmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val recipient = intent.getStringExtra(EXTRA_RECIPIENT).orEmpty()
        val body = intent.getStringExtra(EXTRA_BODY).orEmpty()
        if (recipient.isBlank() || body.isBlank()) return
        SmsUtils.sendSms(recipient, body)
        SmsUtils.refreshMessages(context)
    }

    companion object {
        const val EXTRA_RECIPIENT = "recipient"
        const val EXTRA_BODY = "body"
    }
}
