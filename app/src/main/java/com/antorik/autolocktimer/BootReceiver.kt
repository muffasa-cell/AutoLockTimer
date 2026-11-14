package com.antorik.autolocktimer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val prefs = context.getSharedPreferences(TimerService.PREFS, Context.MODE_PRIVATE)
        val endAt = prefs.getLong(TimerService.KEY_END_AT, 0L)
        if (endAt <= 0L) return

        if (System.currentTimeMillis() >= endAt) {
            TimerAlarmReceiver().onReceive(context, null)
            return
        }

        val serviceIntent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_RESUME
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
