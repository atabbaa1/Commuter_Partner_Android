package com.abdulrahmantabbaa.commuterpartner

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.random.Random

class LocationService: Service() {

    private val INTERVAL: Long = 10000L // time between location updates in ms
    // A coroutine is a concurrency design pattern which simplifies code that runs asynchronously
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationClient: LocationClient
    private var ringtone: Ringtone? = null

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        locationClient = DefaultLocationClient(
            applicationContext,
            LocationServices.getFusedLocationProviderClient(applicationContext)
        )
    }

    // This function is called for every Intent we send to the service
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // if the intent's action is ACTION_START, call start().
        // if the intent's action is ACTION_STOP, call stop().
        when (intent?.action) {
            ACTION_START -> start()
            ACTION_STOP -> stop()
            STOP_RINGTONE -> stopRingtone()

        }
        return super.onStartCommand(intent, flags, startId)
    }

    // Calculate the distance in meters between two (lat,long) pairs using Haversine formula
    private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val R = 6378137 // Earth's mean radius in meters
        val dLat = (lat2 - lat1) * kotlin.math.PI / 180 / 2
        val dLng = (lng2 - lng1) * kotlin.math.PI / 180 / 2
        val sqrtTerm = kotlin.math.sin(dLat).pow(2) + kotlin.math.cos(lat1 * kotlin.math.PI/180) * kotlin.math.cos(lat2 * kotlin.math.PI/180) * kotlin.math.sin(dLng).pow(2)
        return R * 2 * kotlin.math.atan2(kotlin.math.sqrt(sqrtTerm), kotlin.math.sqrt(1 - sqrtTerm))
    }

    private fun start() {
        // TODO: Eventually, remove the changing notification *****************************************************
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Tracking location...")
            .setContentText("Location: null")
            .setSmallIcon(R.mipmap.commuter_partner_icon_foreground)
            .setOngoing(true)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        startForeground(FOREGROUND_ID, notification.build())
        val appContext = applicationContext as LocationApp // Ensure LocationApp extends Application and implements ViewModelStoreOwner
        locationClient.getLocationUpdates(INTERVAL)
            .catch { e -> e.printStackTrace() }
            .onEach { location ->
                // TODO: Fix the random later! Included so that position changes and MutableStateFlow in LocationRepository updates
                // val lat = location.latitude + Random.nextDouble(0.0, 1.0)
                // val long = location.longitude + Random.nextDouble(0.0, 1.0)
                val lat = location.latitude
                val long = location.longitude
                val updatedNotification = notification.setContentText("Location: ($long, $lat)")
                notificationManager.notify(FOREGROUND_ID, updatedNotification.build())
                // Log.d("LocationService", "User is at: ($lat, $long)")
                // If this Service needs continuous updates, wrap the logic in
                // CoroutineScope(Dispatchers.Main).launch {
                //   LocationRepository.locationFlow.collect { circleData -> ___ } }

                // If the Service only needs one-time update, perform the following block of code
                // Read the LocationRepository circle center and radius
                val circleData = LocationRepository.locationFlow.value
                val circleLat = circleData.lat
                val circleLong = circleData.long
                val circleRadius = circleData.radius
                val arrived = circleRadius >= calculateDistance(lat, long, circleLat, circleLong)
                if (arrived) {
                    // Log.d("LocationService", "User is inside the circle!")
                    // Generate the notification sound
                    val uri = LocationRepository.ringtoneFlow.value
                    if (uri != null) {
                        playRingtone(uri)
                    } else {
                        // Shouldn't really ever occur
                        stopRingtone()
                    }
                    // Send a new notification
                    val arrivedNotification = notification.setContentText("You have arrived at your destination!")
                    notificationManager.notify(FOREGROUND_ID, arrivedNotification.build())
                    // Update the LocationRepository
                    LocationRepository.updateLocation(circleLat, circleLong, circleRadius, true)
                    stop()
                }
            }
            .launchIn(serviceScope)
    }

    private fun stop() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        locationClient.pauseLocationUpdates()
        // serviceScope.cancel() // Cancel the locationClient.getLocationUpdates and thereby the tracking
        // stopSelf() // This line calls onDestroy()
    }

    // When the service is destroyed, cancel the serviceScope: CoroutineScope
    // Since this LocationService calls DefaultLocationClient which attaches the
    // callbackFlow, as soon as the Service is destroyed and the serviceScope is
    // cancelled, the app automatically stops tracking location
    override fun onDestroy() {
        // stopRingtone()
        super.onDestroy()
        serviceScope.async {  }
    }

    private fun playRingtone(uri: Uri) {
        ringtone = RingtoneManager.getRingtone(applicationContext, uri)
        ringtone?.play()
    }

    private fun stopRingtone() {
        ringtone?.stop()
        // Stop tracking the user
        // stop()
    }

    // Allows the constants inside to be accessible outside this class with the dot '.' operator
    // Ex) LocationService.ACTION_START
    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val STOP_RINGTONE = "STOP_RINGTONE"
        const val NOTIFICATION_CHANNEL_ID = "location"
        const val FOREGROUND_ID: Int = 1
    }
}