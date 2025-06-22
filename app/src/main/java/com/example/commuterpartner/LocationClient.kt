package com.example.commuterpartner

import android.location.Location
import kotlinx.coroutines.flow.Flow

interface LocationClient {
    fun getLocationUpdates(interval: Long): Flow<Location>
    fun pauseLocationUpdates()

    class locationException(message: String): Exception()
}