package com.example.smsapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.example.smsapp.util.SmsUtils

class SmsReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reply = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(KEY_TEXT)
            ?.toString()
            .orEmpty()
        val recipient = intent.getStringExtra(EXTRA_RECIPIENT).orEmpty()
        if (reply.isBlank() || recipient.isBlank()) return
        SmsUtils.sendSmsToRecipients(recipient, reply)
        SmsUtils.refreshMessages(context)
    }

    companion object {
        const val KEY_TEXT = "reply_text"
        const val EXTRA_RECIPIENT = "reply_recipient"
    }
}
