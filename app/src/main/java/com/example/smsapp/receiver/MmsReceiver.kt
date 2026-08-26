package com.example.smsapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.smsapp.util.SmsUtils

class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Minimal stub to satisfy default SMS role; you can expand to process MMS content later.
        Log.d("MmsReceiver", "Received MMS/WAP push intent: ${intent.action}")
        SmsUtils.refreshMessages(context)
    }
}
