package com.example.commuterpartner

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.commuterpartner.databinding.ActivityMapsBinding
import com.google.android.gms.maps.model.MapColorScheme
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import androidx.core.content.ContextCompat

import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.app.NotificationCompat
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.GoogleMap.OnMapClickListener
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.Marker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// In the following function header ':' stands for extends and ',' stands for implements
class MapsActivity : AppCompatActivity(), OnMapReadyCallback, OnRequestPermissionsResultCallback,
    OnMarkerClickListener, OnMapLongClickListener, OnMapClickListener {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private var permissionDenied = false
    private val DEFAULT_RADIUS: Double = 800.0
    private val MIN_RADIUS: Double = 0.0
    private val MAX_RADIUS: Double = 5000.0
    var currLocation: Location ?= null
    val fusedLocationProviderClient: FusedLocationProviderClient ?= null
    private val DEFAULT_RINGTONE = "Falling Star"

    private lateinit var targetAcquiredBtn: Button
    private var activeMarker: Marker ?= null // Declaring activeMarker as type Marker, and initializing to null. It can be assigned a value null later on, too
    private lateinit var circle: Circle // Declaring circle as type Circle. No default value and can never be null. Use if (::circle.isInitialized)
    private var targetAcquired = false // This reveals whether a Marker has been designated for notification
    private lateinit var circleRadSeekBar: SeekBar
    private lateinit var settingsBtn: Button
    private lateinit var ringtonePickerLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        // setContentView(R.layout.activity_maps)
        setContentView(binding.root) // binding.root is the layout file (contains widgets like Buttons, TextView, etc.)

        // To use a GoogleMap object, there must be either a SupportMapFragment or MapView object as
        // a container object for the map. The GoogleMap object is then retrieved from the container
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        // The SupportMapFragment object can be added dynamically or statically. Dynamically allows for additional
        // actions, like replacing and removing at runtime.
        // Below, the SupportMapFragment object is being added statically. This means we define the
        // fragment in the activity_maps.xml layout file. Below, we get a handle to the fragment by
        // calling the findFragmentById() method and passing it the resource ID of the fragment in the layout file
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this) // Calling this method sets the callback on the fragment

        lifecycleScope.launch (Dispatchers.Main) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { // keeps collecting even when app goes background <-> foreground
                // LocationRepository is the object containing the user location.
                // locationFlow is the name of the Flow in LocationRepository
                // This block of code gets executed every time the LocationRepository changes
                LocationRepository.locationFlow.collect { (lat, long, radius, arrived) ->
                    /**
                     * The code which updates the UI once the user has entered the circle radius
                     */
                    Log.d("MapsActivity", "User is at: ($lat, $long)")
                    if (::circle.isInitialized && arrived) { // If the user entered the circle
                        Log.d("MapsActivity", "User is now inside the circle!")
                        targetAcquired = false
                        targetAcquiredBtn.text = "Notify Me Upon Arrival"
                        activeMarker = null
                        circle.isVisible = false
                        circleRadSeekBar.visibility = View.INVISIBLE

                        Log.d("MapsActivity", "About to generate arrivedDialog")
                        val arrivedDialog = AlertDialog.Builder(this@MapsActivity)
                            .setTitle("Arrived")
                            .setMessage("You have arrived at your destination!")
                            .setPositiveButton("OK") { _, _ ->
                                Intent(applicationContext, LocationService::class.java).apply {
                                    action = LocationService.STOP_RINGTONE
                                    startForegroundService(this)
                                }
                                Intent(applicationContext, LocationService::class.java).apply {
                                    action = LocationService.ACTION_STOP
                                    startForegroundService(this)
                                }
                            }
                            .setCancelable(false)
                            .create()
                        arrivedDialog.show()
                        // Do NOT stop the Service from here with an Intent. The app just crashes
                        // Also, do NOT update LocationRepository with arrived=false
                        // This creates an infinite loop because then the locationFlow in
                        // LocationService will trigger and update LocationRepository with
                        // arrived=true, which triggers this locationFlow with arrived=false, ...
                    }
                }
            }
        }

        circleRadSeekBar = findViewById(R.id.circle_rad_seek_bar)
        circleRadSeekBar.min = MIN_RADIUS.toInt()
        circleRadSeekBar.max = MAX_RADIUS.toInt()

        settingsBtn = findViewById(R.id.settings)
        settingsBtn.setOnClickListener{
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Ringtone")
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, DEFAULT_RINGTONE) // Optional: Pre-select a ringtone
            ringtonePickerLauncher.launch(intent)
        }

        // Register for the ringtone picker result
        ringtonePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val ringtoneUri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                if (ringtoneUri != null) {
                    // Update the MutableStateFlow in the service with the selected URI
                    lifecycleScope.launch {
                        LocationRepository.updateRingtone(ringtoneUri) // TODO: This might be a problem. Unsure
                    }
                }
            }
        }

        // Requesting permission to send notifications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
    }

    /*
    fun openNotificationSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, LocationService.NOTIFICATION_CHANNEL_ID) // Same as created earlier
            }
        } else {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        }
        context.startActivity(intent)
    }
     */

    /**
     * This callback is triggered when the map is ready to be used.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        // Add a marker in Sydney and move the camera
        val sydney = LatLng(-34.0, 151.0)
        val sydneyMarker = mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        sydneyMarker?.tag = DEFAULT_RADIUS
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
        mMap.isTrafficEnabled = false
        mMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
        mMap.setPadding(10, 250, 10, 10) // left, top, right, bottom
        mMap.mapColorScheme = MapColorScheme.FOLLOW_SYSTEM
        circle = mMap.addCircle(CircleOptions().radius(DEFAULT_RADIUS)
            .fillColor(Color.BLUE).visible(false).center(sydney))
        mMap.setOnMarkerClickListener(this)
        mMap.setOnMapLongClickListener(this)
        mMap.setOnMapClickListener(this) // Accommodates clicks on map as clicking off activeMarker

        // Request permission location from the user
        enableMyLocation()
        if (mMap.isMyLocationEnabled) {
            targetAcquiredBtn = findViewById<Button>(R.id.target_acquired_btn)
            targetAcquiredBtn.text = "Notify Me Upon Arrival"
            targetAcquiredBtn.setOnClickListener {handleTargetAcquired(targetAcquiredBtn)}
        }
    }

    override fun onMapLongClick(p0: LatLng) {
        if (!targetAcquired) {
            val newMarker = mMap.addMarker(MarkerOptions().position(p0).title(p0.latitude.toString() + ", " + p0.longitude.toString()))
            newMarker?.tag = DEFAULT_RADIUS // .tag NEEDS to stay of type Double. DO NOT make it an Int!!!
        }
    }

    override fun onMapClick(p0: LatLng) {
        if (!targetAcquired) {
            activeMarker = null
            circle.isVisible = false
            circleRadSeekBar.visibility = View.INVISIBLE
        } else {
            activeMarker = activeMarker
            circle.center = activeMarker?.position!! // the !! means it is non-null
            circle.isVisible = true
        }
    }


    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        // 1. Check if permissions are granted, if so, enable the my location layer
        if (this.hasLocationPermission()) {
            mMap.isMyLocationEnabled = true
            return
        }
        // 2. If a permission rationale dialog should be shown before requesting permission
        // This occurs when the user has previously denied permission
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) || ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        ) {
            showPermissionRationaleDialog(this, LOCATION_PERMISSION_REQUEST_CODE) {
                // Trigger the permission request when the user agrees
                requestPermissions(
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            }
            return
        }
        // 3. Otherwise, request permission
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun isPermissionGranted (permissions: Array<String>, grantResults: IntArray, targetPermission: String): Boolean {
        val index = permissions.indexOf(targetPermission)
        return (index != -1 && grantResults[index] == PackageManager.PERMISSION_GRANTED)
    }

    // This function gets executed when a permission rationale dialog should be shown before requesting permission
    // Rationale dialogs are shown after users have previously rejected the permission
    private fun showPermissionRationaleDialog(
        context: Context,
        requestCode: Int,
        onPositiveAction: () -> Unit
    ) {
        val dialog = AlertDialog.Builder(context)
            .setTitle("Permission Required")
            .setMessage("Location permission is needed for this feature to work. Please grant it.")
            .setPositiveButton("OK") { _, _ ->
                onPositiveAction()
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
            )
            return
        }
        if (isPermissionGranted(
                permissions,
                grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) || isPermissionGranted(
                permissions,
                grantResults,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        ) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation()
        } else {
            // Permission was denied. Display an error message
            // Display the missing permission error dialog when the fragments resume.
            permissionDenied = true
        }
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        if (permissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError()
            permissionDenied = false
        }
    }

    /**
     * Displays a dialog with error message explaining that the location permission is missing.
     */
    private fun showMissingPermissionError() {
        val errorDialog = AlertDialog.Builder(this)
            .setTitle("Location Permission Denied")
            .setMessage("Location permission has been denied")
            .setNegativeButton("Cancel", null)
            .create()
        errorDialog.show()
    }

    private fun handleTargetAcquired (targetAcquiredBtn: Button) {
        if (activeMarker == null) {
            val noDestinationDialog = AlertDialog.Builder(this)
                .setTitle("No Designated Destination")
                .setMessage("You need to select a marker as a destination to be notified upon arrival.")
                .setPositiveButton("OK") { _, _ ->
                }
                .setCancelable(true)
                .create()
            noDestinationDialog.show()
        } else if (!targetAcquired) {
            targetAcquired = true
            targetAcquiredBtn.text = "Cancel Notification/ Designate a Different Marker"
            // circleRadSeekBar.visibility = View.INVISIBLE // TODO: COMMENT THIS OUT WHEN I WANT TO TEST NOTIFICATION UPON USER ENTERING CIRCLE
            // Update the LocationRepository with the center and radius of the Circle
            LocationRepository.updateLocation(circle.center.latitude, circle.center.longitude, circle.radius, false)
            // To start tracking, we need to send an Intent to our LocationService
            // to trigger the onStartCommand()
            Intent(applicationContext, LocationService::class.java).apply {
                action = LocationService.ACTION_START
                startForegroundService(this)
            }
        } else if (targetAcquired) {
            targetAcquired = false
            targetAcquiredBtn.text = "Notify Me Upon Arrival"
            circleRadSeekBar.visibility = View.VISIBLE
            // Optionally, stop tracking the user
            // To stop tracking, we need to send an Intent to our LocationService
            // to trigger the onStartCommand()
            Intent(applicationContext, LocationService::class.java).apply {
                action = LocationService.ACTION_STOP
                startForegroundService(this)
            }

        }
    }

    /**
     * Click Event Listener for Markers
     */
    override fun onMarkerClick(marker: Marker): Boolean {
        if (targetAcquired) {
            return true
        } else {
            if (activeMarker == null || activeMarker?.title != marker.title ) {
                // Make clicked marker the activeMarker and show circle around it
                activeMarker = marker
                circle.center = marker.position
                circle.isVisible = true
                circle.radius = marker.tag as Double
                // Reveal the SeekBar to modify the radius of the circle
                circleRadSeekBar.visibility = View.VISIBLE
                circleRadSeekBar.progress = (marker.tag as Double).toInt() // "as" just states what the Any? object is. toInt() casts it to an Int
                circleRadSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                        if (fromUser) {
                            circle.radius = progress.toDouble()
                            marker.tag = progress.toDouble()
                            LocationRepository.updateLocation(circle.center.latitude, circle.center.longitude, circle.radius, false) // TODO: Remove this later. Allows for circle changes to transmit to LocationRepository
                        }
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    }
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    }
                })
                return false // Maintain default behavior when clicked (show info window and pan to center)
            } else {
                // Clicked marker is the same as current activeMarker. Remove its activeness and hide circle
                activeMarker = null
                circle.isVisible = false
                circleRadSeekBar.visibility = View.INVISIBLE
                return true
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MapsActivity", "MapsActivity is being destroyed!")
    }



    companion object {
        /**
         * Request code for location permission request.
         *
         * @see .onRequestPermissionsResult
         */
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val RINGTONE_PERMISSION_REQUEST_CODE = 3
    }
}

// Intents are objects of the android.content.Intent type. Your code can send them to the Android system defining
// the components you are targeting. Intent to start an activity called CircleActivity with the following code.
// val intent = Intent(this, CircleActivity::class.java)
// start the activity connect to the specified class
// startActivity(intent)