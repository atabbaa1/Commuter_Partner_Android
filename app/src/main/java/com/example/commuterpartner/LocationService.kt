package com.example.commuterpartner

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
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
import kotlin.random.Random

class LocationService: Service() {

    private val INTERVAL: Long = 10000L // time between location updates in ms
    private val FOREGROUND_ID: Int = 1
    // A coroutine is a concurrency design pattern which simplifies code that runs asynchronously
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationClient: LocationClient
    private lateinit var locationViewModel: LocationViewModel

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
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun start() {
        // Eventually, remove the changing notification *****************************************************
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Tracking location...")
            .setContentText("Location: null")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setOngoing(true)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        startForeground(FOREGROUND_ID, notification.build())
        // Initializing LocationView material
        val appContext = applicationContext as LocationApp // Ensure LocationApp extends Application and implements ViewModelStoreOwner
        val locationViewModel = ViewModelProvider(appContext)
            .get(LocationViewModel::class.java)
        locationClient.getLocationUpdates(INTERVAL)
            .catch { e -> e.printStackTrace() }
            .onEach { location ->
                // TODO: Fix the random later!
                val lat = location .latitude + Random.nextDouble(0.0, 1.0)
                val long = location .longitude + Random.nextDouble(0.0, 1.0)
                val updatedNotification = notification.setContentText("Location: ($long, $lat)")
                notificationManager.notify(FOREGROUND_ID, updatedNotification.build())
                // Update the LocationRepository
                LocationRepository.updateLocation(lat, long)
            }
            .launchIn(serviceScope)
    }

    private fun stop() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // When the service is destroyed, cancel the serviceScope: CoroutineScope
    // Since this LocationService calls DefaultLocationClient which attaches the
    // callbackFlow, as soon as the Service is destroyed and the serviceScope is
    // cancelled, the app automatically stops tracking location
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    // Allows the constants inside to be accessible outside this class with the dot '.' operator
    // Ex) LocationService.ACTION_START
    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val NOTIFICATION_CHANNEL_ID = "location"
    }
}