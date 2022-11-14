package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity.RESULT_OK
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.location.Geofence.NEVER_EXPIRE
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.*

private const val TAG = "RemindersActivity"
private const val GEOFENCE_RADIUS_IN_METERS = 30f

class SaveReminderFragment : BaseFragment() {

    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding

    companion object {
        internal const val ACTION_GEOFENCE_EVENT =
            "RemindersActivity.ACTION_GEOFENCE_EVENT"
    }

    private val runningQOrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    lateinit var geofencingClient: GeofencingClient
    private lateinit var reminder: ReminderDataItem

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireActivity(), GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(
            requireActivity(),
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        geofencingClient = LocationServices.getGeofencingClient(requireActivity())

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this

        binding.selectLocation.setOnClickListener {
            //Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

            val id = if (_viewModel.reminderId.value != null) _viewModel.reminderId.value
            else UUID.randomUUID().toString()

            reminder = ReminderDataItem(title, description, location, latitude, longitude, id!!)

            if (_viewModel.validateEnteredData(reminder)) {
                checkLocationPermissionsAndStartGeofencing()
            }
        }
    }

    private fun checkLocationPermissionsAndStartGeofencing() {
        if (isForegroundLocationPermissionsApproved() && isBackgroundLocationPermissionsApproved()) {
            checkDeviceLocationSettingsAndStartGeofence()
        } else {
            if (!isForegroundLocationPermissionsApproved()) {
                requestForegroundLocationPermissions()
            }
            if (!isBackgroundLocationPermissionsApproved()) {
                requestBackgroundLocationPermission()
            }
            if (isForegroundLocationPermissionsApproved() && isBackgroundLocationPermissionsApproved()) {
                checkDeviceLocationSettingsAndStartGeofence()
            }
        }
    }

    private fun requestForegroundLocationPermissions() {
        when {
            isForegroundLocationPermissionsApproved() -> {
                return
            }

            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Snackbar.make(
                    binding.root,
                    R.string.permission_denied_explanation, Snackbar.LENGTH_LONG
                )
                    .setAction(R.string.settings) {
                        startActivity(Intent().apply {
                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    }.show()
            }
            else -> {
                registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
                    if (result.all<String, @JvmSuppressWildcards Boolean> { result -> result.value }) {
                        //granted
                        checkLocationPermissionsAndStartGeofencing()
                        Log.d(TAG, "Permission Granted")

                    } else {
                        //Denied
                        Snackbar.make(
                            binding.root,
                            R.string.permission_denied_explanation, Snackbar.LENGTH_LONG
                        )
                            .setAction(R.string.settings) {
                                startActivity(Intent().apply {
                                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                    data =
                                        Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                })
                            }.show()
                    }
                }.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    private fun isForegroundLocationPermissionsApproved(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @TargetApi(Build.VERSION_CODES.Q)
    private fun isBackgroundLocationPermissionsApproved(): Boolean {
        return if (runningQOrLater) {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @TargetApi(Build.VERSION_CODES.Q)
    private fun requestBackgroundLocationPermission() {
        if (isBackgroundLocationPermissionsApproved()) {
            checkDeviceLocationSettingsAndStartGeofence()
            return
        }
        if (runningQOrLater) {
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
                if (result.all<String, @JvmSuppressWildcards Boolean> { result -> result.value }) {
                    //granted
                    checkLocationPermissionsAndStartGeofencing()
                    Log.d(TAG, "Permission Granted")

                } else {
                    //Denied
                    Snackbar.make(
                        binding.root,
                        R.string.permission_denied_explanation, Snackbar.LENGTH_LONG
                    )
                        .setAction(R.string.settings) {
                            startActivity(Intent().apply {
                                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            })
                        }.show()
                }
            }.launch(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION))

        } else {
            return
        }
    }

    private fun checkDeviceLocationSettingsAndStartGeofence(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = Priority.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())

        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    val intentSenderRequest =
                        IntentSenderRequest.Builder(exception.resolution).build()
                    registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
                        if (result.resultCode == RESULT_OK) {
                            addGeofenceForReminder()
                        }
                    }.launch(intentSenderRequest)

                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                }
            } else {
                Snackbar.make(
                    binding.root,
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndStartGeofence()
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                addGeofenceForReminder()
            }
        }
    }

    private fun addGeofenceForReminder() {

        val geofence = Geofence.Builder()
            .setRequestId(reminder.id)
            .setCircularRegion(
                reminder.latitude!!,
                reminder.longitude!!,
                200f
            )
            .setExpirationDuration(NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
            addOnSuccessListener {
                Log.e("Add Geofence", geofence.requestId)
                _viewModel.validateAndSaveReminder(reminder)
            }
            addOnFailureListener {
                Toast.makeText(
                    requireContext(), R.string.geofences_not_added,
                    Toast.LENGTH_SHORT
                ).show()
                if ((it.message != null)) {
                    Log.w(TAG, it.message.toString())
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }
}
