package com.alarmingapps.alarming

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.PowerManager
import java.util.*



open class ActivateAlarm : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Alarming:Ongoing Alarm")
        wl.acquire()
        soundAlarm(context)
        wl.release()
    }

    private fun soundAlarm(context: Context) {
        val intent = Intent(context.applicationContext, OngoingAlarm::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun setAlarm(context: Context, triggerAtMillis: Long, disableMethod: Int, desiredLocation: Location) {
        val intent = Intent(context, ActivateAlarm::class.java)
        intent.putExtra("disableMethod", disableMethod)
        intent.putExtra("desiredLocation", desiredLocation)
        val pi = PendingIntent.getBroadcast(context, 0, intent, 0)
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (triggerAtMillis + 60 * 1000 > Calendar.getInstance().timeInMillis) {
            am.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    }

    fun cancelAlarm(context: Context) {
        val intent = Intent(context, ActivateAlarm::class.java)
        val sender = PendingIntent.getBroadcast(context, 0, intent, 0)
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(sender)
    }
}