package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import android.Manifest
import android.annotation.SuppressLint
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.*

private const val TAG = "SelectLocationFragment"
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
private const val LOCATION_PERMISSION_INDEX = 0
private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding

    private var Poi: PointOfInterest? = null
    private var latitude = 0.0
    private var longitude = 0.0
    private var name = ""
    private var isLocationSelected = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
//        : add the map setup implementation
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        binding.saveLocationBtn.setOnClickListener {
            if (Poi != null || isLocationSelected) {
                onLocationSelected()
            } else {
                Toast.makeText(context, "Select a location !", Toast.LENGTH_LONG).show()
            }
        }
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // gets the user's current location
        zoomToUserLocation()
        setMapLongClick(map)
        setPoiClick(map)
        setMapStyle(map)
        requestForegroundLocationPermissions()
    }

    @SuppressLint("MissingPermission")
    private fun zoomToUserLocation() {
        if (isForegroundLocationPermissionsApproved()) {
            map.isMyLocationEnabled = true
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        Log.i(
                            TAG,
                            "onMapReady (line 93): latitude: ${location.latitude} longitude: ${location.longitude}"
                        )
                        map.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(
                                    location.latitude,
                                    location.longitude
                                ), 14f
                            )
                        )
                        map.addMarker(
                            MarkerOptions().position(LatLng(location.latitude, location.longitude))
                                .title("Marker in Current Location")
                        )
                    }
                }
        }
    }

    private fun setMapLongClick(map: GoogleMap) {
        map.setOnMapLongClickListener { latLng ->
            map.clear()
            val snippet = String.format(
                Locale.getDefault(),
                "Lat: %1$.5f, Long: %2$.5f",
                latLng.latitude,
                latLng.longitude
            )
            map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.dropped_pin))
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
            latitude = latLng.latitude
            longitude = latLng.longitude
            name = String.format(
                Locale.getDefault(),
                "Lat: %1$.5f, Long: %2$.5f",
                latLng.latitude,
                latLng.longitude
            )
            isLocationSelected = true
        }
    }

    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            map.clear()
            val poiMarker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
            )
            poiMarker?.showInfoWindow()
            Poi = poi
            latitude = poi.latLng.latitude
            longitude = poi.latLng.longitude
            name = poi.name
        }
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.my_map_style
                )
            )
            if (!success) {
                Log.e(TAG, "Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }
    }

    private fun onLocationSelected() {
        _viewModel.latitude.value = latitude
        _viewModel.longitude.value = longitude
        _viewModel.reminderSelectedLocationStr.value = name
        _viewModel.navigationCommand.postValue(NavigationCommand.Back)
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // : Change the map type based on the user's selection.
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    /**
     * Starts the permission check process only if the associated with the
     * current hint isn't yet active.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onStart() {
        super.onStart()
        checkPermissions()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun checkPermissions() {
        checkDeviceLocationSettings()
        if (!isForegroundLocationPermissionsApproved()) {
            Toast.makeText(context, "Location permission not granted enable it", Toast.LENGTH_LONG)
                .show()
            requestForegroundLocationPermissions()
        }
    }

    private fun checkDeviceLocationSettings(resolve: Boolean = true) {
        Log.i(TAG, "checkDeviceLocationSettings (line 240): called")
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
                    exception.startResolutionForResult(
                        requireActivity(),
                        REQUEST_TURN_DEVICE_LOCATION_ON
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                }
            } else {
                Snackbar.make(
                    binding.map,
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettings()
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                zoomToUserLocation()
            }
        }
    }

    /**
     * Request Permissions
     */
    private fun isForegroundLocationPermissionsApproved(): Boolean {
        return (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ))
    }

    @SuppressLint("MissingPermission")
    private fun requestForegroundLocationPermissions() {
        if (isForegroundLocationPermissionsApproved()) {
            map.isMyLocationEnabled = true
            return
        }

        Log.d(TAG, "Request foreground only location permission")

        requestPermissions(
            arrayOf(
                "android.permission.ACCESS_COARSE_LOCATION",
                "android.permission.ACCESS_FINE_LOCATION"
            ), REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE) {
            if (
                grantResults.isNotEmpty() &&
                grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_GRANTED
            ) {
                // Permission granted
                Log.i(TAG, "onRequestPermissionsResult (line 383): permission granted")
                zoomToUserLocation()
            } else {
                // Permission was denied. Display an error message.
                Log.i(TAG, "onRequestPermissionsResult (line 397): permission denied")
                Toast.makeText(
                    requireContext(),
                    R.string.permission_denied_explanation,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
