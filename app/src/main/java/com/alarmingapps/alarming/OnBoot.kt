package com.alarmingapps.alarming

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import java.io.ObjectInputStream

class OnBoot : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        //Check if the caller is system
        //if (Binder.getCallingUid()!=1000) {return}
        //Disabling this check, as the legitimate caller does not have Uid==1000
        //The potential damage that can be done by calling this method is very minor anyway; just an extra alarm.
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val alarmSetter = ActivateAlarm()
            val checkingFile = context.getFileStreamPath(LOCAL_CURRENT_SETTINGS_FILENAME)
            if (!checkingFile.exists()) {return}
            val file = context.openFileInput(LOCAL_CURRENT_SETTINGS_FILENAME)
            val `in` = ObjectInputStream(file)
            val data = `in`.readObject() as Map<*, *>
            alarmSetter.cancelAlarm(context)
            val desiredLocation = Location("")
            desiredLocation.latitude = data["latitude"] as Double
            desiredLocation.longitude = data["longitude"] as Double
            if (data["enabled"] as Boolean) {alarmSetter.setAlarm(context, data["epochMillis"] as Long, data["disableMethod"] as Int, desiredLocation)}
            file.close()
        }
    }
}