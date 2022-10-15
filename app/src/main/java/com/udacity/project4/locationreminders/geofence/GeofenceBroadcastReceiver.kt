package com.udacity.project4.locationreminders.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.udacity.project4.R
import com.udacity.project4.locationreminders.geofence.GeofenceTransitionsJobIntentService.Companion.enqueueWork
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.savereminder.SaveReminderFragment.Companion.ACTION_GEOFENCE_EVENT
import com.udacity.project4.utils.sendNotification

/**
 * Triggered by the Geofence.  Since we can have many Geofences at once, we pull the request
 * ID from the first Geofence, and locate it within the cached data in our Room DB
 *
 * Or users can add the reminders and then close the app, So our app has to run in the background
 * and handle the geofencing in the background.
 * To do that you can use https://developer.android.com/reference/android/support/v4/app/JobIntentService to do that.
 *
 */
private const val TAG = "GeofenceBroadcastReceiver"

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "onReceive (line 32): onReceive called")
        // : Step 11 implement the onReceive method
        if (intent.action == ACTION_GEOFENCE_EVENT) {
            val geofencingEvent = GeofencingEvent.fromIntent(intent)

            if (geofencingEvent != null) {
                if (geofencingEvent.hasError()) {
                    Log.e(TAG, geofencingEvent.errorCode.toString())
                    return
                }
            }
            if (geofencingEvent != null) {
                if (geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                    Log.v(TAG, context.getString(R.string.geofence_entered))
                    enqueueWork(context, intent)
                    val geofenceId = when {
                        geofencingEvent.triggeringGeofences?.isNotEmpty() == true ->{
                            Log.i(TAG, "onReceive (line 48): ${geofencingEvent.triggeringGeofences!![0].requestId}")
                            geofencingEvent.triggeringGeofences?.get(0)?.requestId
                        }
                        else -> {
                            Log.e(TAG, "No Geofence Trigger Found! Abort mission!")
                            return
                        }
                    }
                    val reminder = ReminderDataItem(
                        title = geofenceId,
                        description = geofenceId,
                        location = geofenceId,
                        latitude = geofencingEvent.triggeringLocation?.latitude,
                        longitude = geofencingEvent.triggeringLocation?.longitude
                    )
                    sendNotification(
                        context, reminder
                    )
                }
            }
        }
    }
}