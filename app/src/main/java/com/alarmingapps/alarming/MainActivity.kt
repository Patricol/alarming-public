package com.alarmingapps.alarming

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.app.TimePickerDialog
import android.content.Context
import android.icu.text.DateFormat
import android.icu.util.Calendar
import android.view.View
import android.view.View.OnFocusChangeListener
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Handler
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.widget.*
import android.widget.AdapterView
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import java.io.*
import java.util.*


const val LOG_TAG = "Alarming MainActivity"
const val MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1

const val LOCAL_EXPORTED_SETTINGS_FILENAME = "exported_settings"
const val LOCAL_NEXT_ALARM_FILENAME = "next_alarm"
const val LOCAL_CURRENT_SETTINGS_FILENAME = "current_settings"

const val DISABLE_BY_BUTTON = 0
const val DISABLE_BY_LOCATION = 1
/*const val DISABLE_BY_QR_CODE = 2
const val DISABLE_BY_SHAKE = 3
const val DISABLE_BY_FINGERPRINT = 4
const val DISABLE_BY_NFC = 5*/


class MainActivity : AppCompatActivity() {

    private val alarmSetter = ActivateAlarm()
    private val signInRequestCode = 123 //the request code could be any Integer
    val alarm = Alarm("", Calendar.getInstance().timeInMillis, DISABLE_BY_BUTTON, false, makeFakeLocation())
    private var syncingSettings: SyncingData? = null

    private val handler: Handler = Handler()
    private val delay: Long = 3*1000
    private val handleChanges = object: Runnable {
        override fun run() {
            if (syncingSettings != null && syncingSettings!!.localHasChanged()) {
                updatePage()
                syncingSettings!!.acknowledgeLocalChanges()
            }
            handler.postDelayed(this, delay)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<EditText>(R.id.DateField).onFocusChangeListener = OnFocusChangeListener { v, hasFocus -> onDateFocusChange(v, hasFocus)}
        findViewById<EditText>(R.id.TimeField).onFocusChangeListener = OnFocusChangeListener { v, hasFocus -> onTimeFocusChange(v, hasFocus)}
        updatePage()//put before setContentView?

        findViewById<EditText>(R.id.DescriptionField).addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {alarm.description = s.toString()}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })

        setSupportActionBar(findViewById(R.id.toolbar))

        val disableMethodSpinner = findViewById<Spinner>(R.id.DisableMethodSpinner)
        val adapter = ArrayAdapter.createFromResource(this, R.array.disable_methods, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        disableMethodSpinner.adapter = adapter


        disableMethodSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                alarm.disableMethod = position
                updatePage()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        findViewById<Switch>(R.id.EnableAlarm).setOnCheckedChangeListener {_, isChecked ->  onEnabledChanged(isChecked)}

        restoreSettingsFromStorage()
        updatePage()

        findViewById<Button>(R.id.firebase_login_button).setOnClickListener{
            if (!isSignedIn()) {signIn()}
        }
        findViewById<Button>(R.id.firebase_logout_button).setOnClickListener{
            if (isSignedIn()) {signOut()}
        }
        findViewById<Button>(R.id.upload_settings).setOnClickListener{
            if (isSignedIn()) {uploadSettings()}
        }
        findViewById<Button>(R.id.download_settings).setOnClickListener{
            if (isSignedIn()) {downloadSettings()}
        }
        findViewById<Button>(R.id.export_settings).setOnClickListener{
            exportSettingsToStorage()
        }
        findViewById<Button>(R.id.import_settings).setOnClickListener{
            importSettingsFromStorage()
        }
        findViewById<Button>(R.id.require_current_location).setOnClickListener{
            saveLocation()
        }
    }

    override fun onResume() {
        handler.postDelayed(handleChanges, 0)
        super.onResume()
    }

    override fun onPause() {
        handler.removeCallbacks(handleChanges)
        super.onPause()
    }

    private fun adjustAlarm() {
        alarmSetter.cancelAlarm(this)
        if (alarm.enabled) {
            alarmSetter.setAlarm(this, alarm.epochMillis, alarm.disableMethod, alarm.desiredLocation)
            saveNextAlarmToStorage()
        }
        saveCurrentSettingsToStorage()
    }

    private fun onEnabledChanged(isChecked: Boolean) {
        alarm.enabled = isChecked
        adjustAlarm()
    }

    private fun onTimeFocusChange(v: View, hasFocus: Boolean) {
        if (hasFocus) {v.clearFocus()}
        else {
            val alarmTime = alarm.getDateTime()
            val alarmHourOfDay = alarmTime.get(Calendar.HOUR_OF_DAY)
            val alarmMinute = alarmTime.get(Calendar.MINUTE)
            TimePickerDialog(this,
                    TimePickerDialog.OnTimeSetListener{_, hourOfDay, minute ->
                        onTimeSet(hourOfDay, minute)},
                    alarmHourOfDay, alarmMinute, true).show()
        }
    }

    private fun onTimeSet(hourOfDay: Int, minute: Int) {
        alarm.setTime(hourOfDay, minute)
        onDateTimeChanged()
    }

    private fun onDateFocusChange(v: View, hasFocus: Boolean) {
        if (hasFocus) {v.clearFocus()}
        else {
            val alarmTime = alarm.getDateTime()
            val alarmYear = alarmTime.get(Calendar.YEAR)
            val alarmMonth = alarmTime.get(Calendar.MONTH)
            val alarmDayOfMonth = alarmTime.get(Calendar.DAY_OF_MONTH)
            DatePickerDialog(this,
                    DatePickerDialog.OnDateSetListener{_, year, month, dayOfMonth ->
                        onDateSet(year, month, dayOfMonth)},
                    alarmYear, alarmMonth, alarmDayOfMonth).show()
        }
    }

    private fun onDateSet(year: Int, month: Int, dayOfMonth: Int) {
        alarm.setDate(year, month, dayOfMonth)
        onDateTimeChanged()
    }

    private fun onDateTimeChanged() {
        updatePage()
        adjustAlarm()
    }

    private fun checkFileExists(filename: String): Boolean {
        val file = this.getFileStreamPath(filename)
        return file.exists()
    }

    private fun exportToStorage(filename: String, data: HashMap<String, Any>) {
        this.openFileOutput(filename, Context.MODE_PRIVATE).use {
            ObjectOutputStream(it).writeObject(data)
            it.close()
        }
    }

    private fun saveCurrentSettingsToStorage() {
        exportToStorage(LOCAL_CURRENT_SETTINGS_FILENAME, alarm.getData())
    }

    private fun exportSettingsToStorage() {
        exportToStorage(LOCAL_EXPORTED_SETTINGS_FILENAME, alarm.getData())
        updatePage()
    }

    private fun saveNextAlarmToStorage() {
        exportToStorage(LOCAL_NEXT_ALARM_FILENAME, alarm.getDataForExecution())
    }

    private fun importFromStorage(filename: String) : Map<*, *> {
        val file = this.openFileInput(filename)
        val `in` = ObjectInputStream(file)
        val data = `in`.readObject() as Map<*, *>
        file.close()
        return data
    }

    private fun overwriteAlarmFromFile(filename: String) {
        if (checkFileExists(filename)) {
            val data = importFromStorage(filename)
            alarm.description = data["description"] as String
            alarm.epochMillis = data["epochMillis"] as Long
            alarm.disableMethod = data["disableMethod"] as Int
            alarm.desiredLocation = Location("")
            alarm.desiredLocation.latitude = data["latitude"] as Double
            alarm.desiredLocation.longitude = data["longitude"] as Double
            alarm.enabled = data["enabled"] as Boolean
        }
        updatePage()
    }

    private fun restoreSettingsFromStorage() {
        overwriteAlarmFromFile(LOCAL_CURRENT_SETTINGS_FILENAME)
    }

    private fun importSettingsFromStorage() {
        overwriteAlarmFromFile(LOCAL_EXPORTED_SETTINGS_FILENAME)
    }

    private fun showSnackbar(id : Int){
        Snackbar.make(findViewById(R.id.MainLayout), resources.getString(id), Snackbar.LENGTH_LONG).show()
    }

    private fun updatePage() {
        if (isSignedIn()) {
            findViewById<Button>(R.id.firebase_login_button).visibility = View.GONE
            findViewById<Button>(R.id.firebase_logout_button).visibility = View.VISIBLE
            findViewById<Button>(R.id.upload_settings).visibility = View.VISIBLE
            findViewById<Button>(R.id.download_settings).visibility = if (uploadedSettingsExist()) View.VISIBLE else View.GONE
        } else {
            findViewById<Button>(R.id.firebase_login_button).visibility = View.VISIBLE
            findViewById<Button>(R.id.firebase_logout_button).visibility = View.GONE
            findViewById<Button>(R.id.upload_settings).visibility = View.GONE
            findViewById<Button>(R.id.download_settings).visibility = View.GONE
        }
        findViewById<Button>(R.id.import_settings).visibility = if (checkFileExists(LOCAL_EXPORTED_SETTINGS_FILENAME)) View.VISIBLE else View.GONE
        findViewById<Button>(R.id.require_current_location).visibility = if (findViewById<Spinner>(R.id.DisableMethodSpinner).selectedItemPosition == DISABLE_BY_LOCATION) View.VISIBLE else View.GONE
        findViewById<EditText>(R.id.TimeField).setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(alarm.getDateTime()))
        findViewById<EditText>(R.id.DateField).setText(DateFormat.getDateInstance(DateFormat.LONG).format(alarm.getDateTime()))
        //set UI things to match alarm.
        findViewById<Switch>(R.id.EnableAlarm).isChecked = alarm.enabled
        findViewById<Spinner>(R.id.DisableMethodSpinner).setSelection(alarm.disableMethod)
        findViewById<EditText>(R.id.DescriptionField).setText(alarm.description)
    }

    private fun getUser(): FirebaseUser? {
        return FirebaseAuth.getInstance().currentUser
    }

    private fun isSignedIn(): Boolean {
        return (getUser() != null)
    }

    private fun signIn() {
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setIsSmartLockEnabled(!BuildConfig.DEBUG)
                        .setAvailableProviders(Arrays.asList(
                                //AuthUI.IdpConfig.GoogleBuilder().build(),//Disabling for public repository; SHA-1 fingerprints need to match.
                                AuthUI.IdpConfig.EmailBuilder().build()/*,//Add these later
                                AuthUI.IdpConfig.PhoneBuilder().build(),
                                AuthUI.IdpConfig.FacebookBuilder().build(),
                                AuthUI.IdpConfig.TwitterBuilder().build()*/))
                        .build(),
                signInRequestCode)
    }

    private fun signOut() {
        AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener{updatePage()}
        syncingSettings = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == signInRequestCode) {
            val response = IdpResponse.fromResultIntent(data)

            when {
                resultCode == Activity.RESULT_OK -> updatePage()
                response == null -> showSnackbar(R.string.sign_in_cancelled)// User pressed back button
                response.error!!.errorCode == ErrorCodes.NO_NETWORK -> showSnackbar(R.string.no_internet_connection)
                else -> {
                    showSnackbar(R.string.unknown_error)
                    Log.e("Alarming FirebaseLogin", "Sign-in error: ", response.error)
                }
            }
            return
        }
    }

    private fun ensureSyncingSettings() {
        if (syncingSettings == null) {
            syncingSettings = getUserSyncingData()
        }
    }

    private fun uploadedSettingsExist(): Boolean {
        ensureSyncingSettings()
        return !syncingSettings!!.localData.isEmpty()
    }

    private fun uploadSettings() {
        ensureSyncingSettings()
        syncingSettings!!.lockAutoUpdatingLocal()
        syncingSettings!!.localData = alarm.getDataWithUID()
        syncingSettings!!.uploadChanges()
        syncingSettings!!.unlockAutoUpdatingLocal()
        updatePage()
    }

    private fun downloadSettings() {
        ensureSyncingSettings()
        syncingSettings!!.overwriteLocal()

        val data = syncingSettings!!.localData

        alarm.description = data["description"] as String
        alarm.epochMillis = data["epochMillis"] as Long
        alarm.disableMethod = (data["disableMethod"] as Long).toInt()
        alarm.desiredLocation = Location("")
        alarm.desiredLocation.latitude = data["latitude"] as Double
        alarm.desiredLocation.longitude = data["longitude"] as Double
        alarm.enabled = data["enabled"] as Boolean

        updatePage()
    }

    private fun askForLocationPermission() {
        ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {
            // If request is cancelled, the result arrays are empty.
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {saveLocation()}
        }
    }

    private fun makeFakeLocation(): Location {
        val fakeLocation = Location("")
        fakeLocation.latitude = 0.0
        fakeLocation.longitude = 0.0
        return fakeLocation
    }

    private fun saveLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            askForLocationPermission()

        } else {
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (currentLocation == null) {
                showSnackbar(R.string.bad_location)
                alarm.desiredLocation = makeFakeLocation()
            } else {
                alarm.desiredLocation = currentLocation
                Toast.makeText(this, "Location Set", Toast.LENGTH_LONG).show()
            }
        }
        adjustAlarm()
    }

}