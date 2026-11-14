package com.antorik.autolocktimer

import android.Manifest
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var hours: EditText
    private lateinit var minutes: EditText
    private lateinit var status: TextView
    private val requestAdmin = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        requestNotificationPermissionIfNeeded()
        refreshStatus()
    }

    private fun buildUi() {
        val pad = (22 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(pad, pad * 2, pad, pad)
        }

        val title = TextView(this).apply {
            text = "AutoLock Timer"
            textSize = 28f
            gravity = Gravity.CENTER
        }
        root.addView(title, matchWrap())

        val subtitle = TextView(this).apply {
            text = "Set a time. Use any app. Your phone will lock automatically."
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(0, pad / 2, 0, pad)
        }
        root.addView(subtitle, matchWrap())

        val timeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        hours = EditText(this).apply {
            hint = "Hours"
            inputType = 2
            setText("0")
            gravity = Gravity.CENTER
        }
        minutes = EditText(this).apply {
            hint = "Minutes"
            inputType = 2
            setText("15")
            gravity = Gravity.CENTER
        }
        timeRow.addView(hours, LinearParams(0, -2, 1f).apply { marginEnd = pad / 3 })
        timeRow.addView(minutes, LinearParams(0, -2, 1f))
        root.addView(timeRow, matchWrap())

        val start = Button(this).apply {
            text = "START TIMER"
            setOnClickListener { startTimer() }
        }
        root.addView(start, LinearParams(-1, -2).apply { topMargin = pad })

        val stop = Button(this).apply {
            text = "CANCEL TIMER"
            setOnClickListener { cancelTimer() }
        }
        root.addView(stop, LinearParams(-1, -2))

        status = TextView(this).apply {
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, pad, 0, 0)
        }
        root.addView(status, matchWrap())

        val permission = Button(this).apply {
            text = "ENABLE SCREEN-LOCK PERMISSION"
            setOnClickListener { requestAdminPermission() }
        }
        root.addView(permission, LinearParams(-1, -2).apply { topMargin = pad })

        val note = TextView(this).apply {
            text = "First use: enable Device Admin permission. Keep notifications allowed so you can see the countdown."
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(0, pad, 0, 0)
        }
        root.addView(note, matchWrap())
        setContentView(root)
    }

    private fun startTimer() {
        val h = hours.text.toString().toLongOrNull() ?: 0L
        val m = minutes.text.toString().toLongOrNull() ?: 0L
        val duration = (h * 60L + m) * 60_000L
        if (duration <= 0L || duration > 7L * 24L * 60L * 60L * 1000L) {
            Toast.makeText(this, "Set a time between 1 minute and 7 days.", Toast.LENGTH_LONG).show()
            return
        }
        if (!isAdminActive()) {
            Toast.makeText(this, "Enable screen-lock permission first.", Toast.LENGTH_LONG).show()
            requestAdminPermission()
            return
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
        }
        val intent = Intent(this, TimerService::class.java)
            .putExtra(TimerService.EXTRA_DURATION, duration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
        else startService(intent)
        Toast.makeText(this, "Timer started. You can use other apps now.", Toast.LENGTH_LONG).show()
        refreshStatus()
    }

    private fun cancelTimer() {
        stopService(Intent(this, TimerService::class.java).setAction(TimerService.ACTION_STOP))
        getSharedPreferences(TimerService.PREFS, Context.MODE_PRIVATE)
            .edit().remove(TimerService.KEY_END_AT).apply()
        TimerService.cancelAlarm(this)
        Toast.makeText(this, "Timer cancelled.", Toast.LENGTH_SHORT).show()
        refreshStatus()
    }

    private fun requestAdminPermission() {
        if (isAdminActive()) {
            Toast.makeText(this, "Screen-lock permission is already enabled.", Toast.LENGTH_SHORT).show()
            return
        }
        val component = ComponentName(this, LockDeviceAdminReceiver::class.java)
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "This permission lets AutoLock Timer lock your phone when your timer ends.")
        }
        startActivityForResult(intent, requestAdmin)
    }

    private fun isAdminActive(): Boolean {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.isAdminActive(ComponentName(this, LockDeviceAdminReceiver::class.java))
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
        }
    }

    private fun refreshStatus() {
        val endAt = getSharedPreferences(TimerService.PREFS, Context.MODE_PRIVATE)
            .getLong(TimerService.KEY_END_AT, 0L)
        status.text = when {
            endAt > System.currentTimeMillis() ->
                "Timer active. Remaining: ${((endAt - System.currentTimeMillis()) / 60000)} min"
            isAdminActive() -> "Screen-lock permission: enabled"
            else -> "Screen-lock permission: not enabled"
        }
    }

    override fun onResume() {
        super.onResume()
        if (::status.isInitialized) refreshStatus()
    }

    private fun matchWrap() = LinearParams(-1, -2)
    private fun LinearParams(width: Int, height: Int, weight: Float = 0f) =
        LinearLayout.LayoutParams(width, height, weight)
}
