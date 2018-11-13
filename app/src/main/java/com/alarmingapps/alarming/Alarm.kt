package com.alarmingapps.alarming

import android.icu.util.Calendar
import android.location.Location
import com.google.firebase.auth.FirebaseAuth
import java.util.HashMap

data class Alarm(var description: String, var epochMillis: Long, var disableMethod: Int, var enabled: Boolean, var desiredLocation: Location) {
    private fun setDateTime(calendar: Calendar) {
        this.epochMillis = calendar.timeInMillis
    }

    fun getDateTime(): Calendar {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = epochMillis
        return calendar
    }

    fun setTime(hourOfDay: Int, minute: Int) {
        val calendar = getDateTime()
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
        calendar.set(Calendar.MINUTE, minute)
        setDateTime(calendar)
    }

    fun setDate(year: Int, month: Int, dayOfMonth: Int) {
        val calendar = getDateTime()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month)
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        setDateTime(calendar)
    }

    fun getDataForExecution(): HashMap<String, Any> {
        return hashMapOf(
                "disableMethod" to disableMethod,
                "latitude" to desiredLocation.latitude,
                "longitude" to desiredLocation.longitude
        )
    }

    fun getData(): HashMap<String, Any> {
        return hashMapOf(
                "description" to description,
                "epochMillis" to epochMillis,
                "disableMethod" to disableMethod,
                "latitude" to desiredLocation.latitude,
                "longitude" to desiredLocation.longitude,
                "enabled" to enabled
        )
    }

    fun getDataWithUID(): HashMap<String, *> {
        val data = getData()
        data["author_id"] = FirebaseAuth.getInstance().currentUser!!.uid
        return data
    }
}