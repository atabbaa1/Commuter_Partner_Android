package com.example.commuterpartner

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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
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

    private fun start() {
        // TODO: Eventually, remove the changing notification *****************************************************
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Tracking location...")
            .setContentText("Location: null")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setOngoing(true)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        startForeground(FOREGROUND_ID, notification.build())
        val appContext = applicationContext as LocationApp // Ensure LocationApp extends Application and implements ViewModelStoreOwner
        locationClient.getLocationUpdates(INTERVAL)
            .catch { e -> e.printStackTrace() }
            .onEach { location ->
                // TODO: Fix the random later! Included so that position changes and MutableStateFlow in LocationRepository updates
                val lat = location.latitude + Random.nextDouble(0.0, 1.0)
                val long = location.longitude + Random.nextDouble(0.0, 1.0)
                val updatedNotification = notification.setContentText("Location: ($long, $lat)")
                notificationManager.notify(FOREGROUND_ID, updatedNotification.build())
                Log.d("LocationService", "User is at: ($lat, $long)")
                // Read the LocationRepository circle center and radius
                // If this Service needs continuous updates, use the following block of code
                /*
                CoroutineScope(Dispatchers.Main).launch {
                    LocationRepository.locationFlow.collect { circleData ->
                        // This block gets executed every time the LocationRepository locationFlow gets updated
                        val circleLat = circleData.lat
                        val circleLong = circleData.long
                        val circleRadius = circleData.radius
                        val arrived = circleRadius + Random.nextDouble(0.0, 150.0) > 900 // TODO: Calculation to see whether the user is now inside the circle
                        Log.d("LocationService", "Arrived is: $arrived")
                        Log.d("LocationService", "circleRadius is: $circleRadius")
                        if (arrived) {
                            Log.d("LocationService", "User is inside the circle!")
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
                            LocationRepository.updateLocation(lat, long, circleRadius, arrived)
                            // Stop tracking the user
                            stop()
                        }
                    }
                }
                 */

                // If the Service only needs one-time update, perform the following block of code
                val circleData = LocationRepository.locationFlow.value
                val circleLat = circleData.lat
                val circleLong = circleData.long
                val circleRadius = circleData.radius
                val arrived = circleRadius + Random.nextDouble(0.0, 150.0) > 900 // TODO: Calculation to see whether the user is now inside the circle
                Log.d("LocationService", "Arrived is: $arrived")
                Log.d("LocationService", "circleRadius is: $circleRadius")
                if (arrived) {
                    Log.d("LocationService", "User is inside the circle!")
                    // Generate the notification sound
                    val uri = LocationRepository.ringtoneFlow.value
                    if (uri != null) {
                        Log.d("LocationService", "The ringtone is NOT null.")
                        playRingtone(uri)
                    } else {
                        // Shouldn't really ever occur
                        stopRingtone()
                    }
                    // Send a new notification
                    val arrivedNotification = notification.setContentText("You have arrived at your destination!")
                    notificationManager.notify(FOREGROUND_ID, arrivedNotification.build())
                    // Update the LocationRepository
                    LocationRepository.updateLocation(lat, long, circleRadius, arrived)
                }
            }
            .launchIn(serviceScope)
    }

    private fun stop() {
        Log.d("LocationService", "Inside stop()")
        stopForeground(STOP_FOREGROUND_REMOVE) // This or the line below calls onDestroy()
        stopSelf() // This or the line below calls onDestroy()
    }

    // When the service is destroyed, cancel the serviceScope: CoroutineScope
    // Since this LocationService calls DefaultLocationClient which attaches the
    // callbackFlow, as soon as the Service is destroyed and the serviceScope is
    // cancelled, the app automatically stops tracking location
    override fun onDestroy() {
        Log.d("LocationService", "Inside onDestroy()")
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun playRingtone(uri: Uri) {
        Log.d("LocationService", "Entering playRingtone() method")
        ringtone = RingtoneManager.getRingtone(applicationContext, uri)
        ringtone?.play()
        Log.d("LocationService", "ringtone playing now")
    }

    private fun stopRingtone() {
        Log.d("LocationService", "Entering stopRingtone() method")
        ringtone?.stop()
        LocationRepository.updateLocation(0.0, 0.0, 0.0, false)
        // Stop tracking the user
        stop()
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