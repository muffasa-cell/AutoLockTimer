package com.antorik.autolocktimer

import android.app.AlarmManager
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class TimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val prefs = context.getSharedPreferences(TimerService.PREFS, Context.MODE_PRIVATE)
        val endAt = prefs.getLong(TimerService.KEY_END_AT, 0L)
        if (endAt == 0L) return

        if (System.currentTimeMillis() < endAt) {
            // An early/inexact delivery is harmless; the foreground service remains the backup.
            return
        }

        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = LockDeviceAdminReceiver::class.java
        if (dpm.isAdminActive(android.content.ComponentName(context, admin))) {
            dpm.lockNow()
        }
        prefs.edit().remove(TimerService.KEY_END_AT).apply()
        TimerService.cancelAlarm(context)
        context.stopService(Intent(context, TimerService::class.java))
    }
}
