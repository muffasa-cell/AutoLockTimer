package com.antorik.autolocktimer

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import java.util.concurrent.TimeUnit

class TimerService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var endAt = 0L

    private val tick = object : Runnable {
        override fun run() {
            val remaining = endAt - System.currentTimeMillis()
            if (remaining <= 0L) {
                lockAndFinish()
            } else {
                updateNotification(remaining)
                handler.postDelayed(this, 1000L)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        if (intent?.action == ACTION_STOP) {
            clearTimer()
            stopSelf()
            return START_NOT_STICKY
        }

        val requestedDuration = intent?.getLongExtra(EXTRA_DURATION, 0L) ?: 0L
        if (requestedDuration > 0L) {
            endAt = System.currentTimeMillis() + requestedDuration
            prefs.edit().putLong(KEY_END_AT, endAt).apply()
            scheduleAlarm(endAt)
        } else {
            endAt = prefs.getLong(KEY_END_AT, 0L)
        }

        if (endAt <= 0L) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification(endAt - System.currentTimeMillis()))
        handler.removeCallbacks(tick)
        handler.post(tick)
        return START_STICKY
    }

    private fun lockAndFinish() {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val component = ComponentName(this, LockDeviceAdminReceiver::class.java)
        if (dpm.isAdminActive(component)) {
            try {
                dpm.lockNow()
            } catch (_: SecurityException) {
                // Keep app state clean even if device policy was revoked.
            }
        }
        clearTimer()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun clearTimer() {
        handler.removeCallbacks(tick)
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().remove(KEY_END_AT).apply()
        cancelAlarm(this)
    }

    private fun scheduleAlarm(time: Long) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = alarmPendingIntent(this)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, time, pi)
            }
        } catch (_: SecurityException) {
            // The foreground service timer remains active as a fallback.
        }
    }

    private fun updateNotification(remaining: Long) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(remaining))
    }

    private fun buildNotification(remaining: Long): Notification {
        val mins = TimeUnit.MILLISECONDS.toMinutes(remaining.coerceAtLeast(0L))
        val secs = TimeUnit.MILLISECONDS.toSeconds(remaining.coerceAtLeast(0L)) % 60
        val openApp = PendingIntent.getActivity(
            this, 2, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle("AutoLock Timer is running")
            .setContentText("Screen locks in %02d:%02d".format(mins, secs))
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Screen lock timer", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val PREFS = "autolock_timer"
        const val KEY_END_AT = "end_at"
        const val EXTRA_DURATION = "duration"
        const val ACTION_RESUME = "com.antorik.autolocktimer.RESUME"
        const val ACTION_STOP = "com.antorik.autolocktimer.STOP"
        private const val CHANNEL_ID = "autolock_timer_channel"
        private const val NOTIFICATION_ID = 41

        fun alarmPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, TimerAlarmReceiver::class.java)
            return PendingIntent.getBroadcast(
                context, 41, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun cancelAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(alarmPendingIntent(context))
        }
    }
}
