package com.alarmingapps.alarming

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.support.v4.content.ContextCompat
import android.widget.Toast
import java.io.ObjectInputStream


class OngoingAlarm : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ongoing_alarm)
        val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        val player = MediaPlayer.create(this, notification)
        player.isLooping = true
        player.start()
        findViewById<Button>(R.id.DisableButton).setOnClickListener{
            if (importNextAlarmDisableMethodFromStorage() != 1 ||
                    locationsAllowDismissing(importNextAlarmLocationFromStorage())) {
                player.stop()
                finish()
            }
        }

    }

    private fun locationsAllowDismissing(desiredLocation: Location): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "No Location Permissions, Allowing Disable", Toast.LENGTH_LONG).show()
            return true
        } else {
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            return if (currentLocation == null) {
                Toast.makeText(this, "No Location Provided by API, Allowing Disable", Toast.LENGTH_LONG).show()
                true
            } else {
                if (currentLocation.distanceTo(desiredLocation) < 100) {
                    Toast.makeText(this, "Within 100 Meters of Target, Allowing Disable", Toast.LENGTH_LONG).show()
                    true
                } else {
                    Toast.makeText(this, "Not Within 100 Meters of Target, Not Allowing Disable", Toast.LENGTH_LONG).show()
                    false
                }
            }
        }
    }

    private fun importNextAlarmDisableMethodFromStorage(): Int {
        val file = this.openFileInput("next_alarm")
        val `in` = ObjectInputStream(file)
        val data = `in`.readObject() as Map<*, *>
        val disableMethod = data["disableMethod"] as Int
        file.close()
        return disableMethod
    }

    private fun importNextAlarmLocationFromStorage(): Location {
        val file = this.openFileInput("next_alarm")
        val `in` = ObjectInputStream(file)
        val data = `in`.readObject() as Map<*, *>
        val desiredLocation = Location("")
        desiredLocation.latitude = data["latitude"] as Double
        desiredLocation.longitude = data["longitude"] as Double
        file.close()
        return desiredLocation
    }
}