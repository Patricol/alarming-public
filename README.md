# alarming-public

Public snapshot of Alarming, a synchronizing alarm clock application.

This initial version was created to practice securely implementing various APIs and methods of storing data.

[screenshot](https://raw.githubusercontent.com/Patricol/alarming-public/master/screenshots/basic.png)

### Table of Contents
* [Setup](#Setup)
* [Features](#Features)
* [Trust Model](#Trust_Model)
* [Threat Model](#Threat_Model)



### Setup <a name="Setup"></a>
* To use the synchronization features, create a Firebase project.
  * add google-services.json to the /app folder etc.
  * enable authentication
    * email authentication specifically; but the app is also setup for Google accounts etc.
  * enable Firestore (Database)
    * set the rules as listed in Cloud_Firestore_Rules.txt
    * add a collection named "users"


### Features <a name="Features"></a>
* Alarming is secure alarm clock application for Android; providing multiple ways to backup and synchronize your settings.
  * Alarming allows you to create an alarm at a specified date and time, with a description and multiple possible options for how it can be disabled.
    * You can simply disable it with the press of a button, or you can require that your phone be in a certain location before the alarm can be disabled.
    * The alarm can also be toggled on and off.
    * All of those settings can be:
      * exported to or imported from storage
      * uploaded to or downloaded from an online database
        * Online synchronization is tied to your Google account or email address.
  * Ongoing alarms loop music until they are disabled, and they do not turn on the phone's screen when it is sleeping, (to save power.)
  * Various error messages account for each of the ways that authentication, location-based-disabling, and location-setting can fail.
  * Alarming starts up on the device's boot, and will sound an alarm as scheduled by the last set configuration, even if it isn't opened in the interim.
  * Alarms interrupt other activites, and can be disabled without needing to unlock the phone.
  * Alarms can be set and tested with any combination of settings.


### Trust Model <a name="Trust_Model"></a>
  * User:
    * I'm trusting the user to not be a malicious actor.
    * I'm trusting the user to not root their device.
    * I'm trusting the user to not type over 1MB of text into the description field, (which would break the Firebase upload.)
  * System:
    * I'm trusting the operating system to not be compromised.
  * Certificates:
    * I'm trusting the root certificates to all be from uncompromised entities.
  * Servers:
    * I'm trusting the firebase servers to give me good data, and to store data as requested.
    * I'm trusting the Google Play Store servers to not replace my application with a malicious one with the same access rights etc.
  * Device:
    * I'm trusting that the device's storage is not completely full, etc.


### Threat Model <a name="Threat_Model"></a>
  * I'm assuming that the attacker does not have physical access to the device.
  * I'm assuming that the attacker does control a non-root process installed on the device, and can send instructions and exfiltrate data via the network.
  * The attacker's motive is to access the user's location data, which is sometimes stored alongside other settings in exported or uploaded settings.
  * A secondary motive would be to prevent the alarm from triggering as it is supposed to.
    * I'm ignoring whole-phone denial of service attacks.
      * E.g.: sending tons of MMS messages to drain the phone's battery.
  * The attack surface includes exposed/exported application interfaces.
    * Those are all restricted by permissions except for the OnBoot handling; which was running into issues where the ACTION_BOOT_COMPLETED handler was not being called by system, but rather by some other user.
      * Leaving OnBoot exposed is a minimal risk though; all it does is ensure the alarm is set properly.
  * The user's (past) location is sometimes stored alongside other settings in exported or uploaded settings; meaning that that data does certainly need to be protected.
  * All synchronized data is protected in transit, and all local data is stored in private locations on internal storage; where they cannot be accessed by non-root actors.
  * The database data is restricted such that it can only be accessed by the same authenticated user who created the data, and authentication happens securely through official APIs.
