package com.meettrack.app.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class LocationResult2(
    val latitude: Double,
    val longitude: Double,
    val formattedAddress: String = ""
)

@Singleton
class LocationService @Inject constructor(
    private val context: Context
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): LocationResult2 = suspendCancellableCoroutine { cont ->
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 10_000L
        ).setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(5_000L)
            .setMaxUpdates(1)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                fusedLocationClient.removeLocationUpdates(this)
                val location = result.lastLocation
                if (location != null) {
                    val address = reverseGeocode(location.latitude, location.longitude)
                    cont.resume(
                        LocationResult2(location.latitude, location.longitude, address)
                    )
                } else {
                    cont.resumeWithException(Exception("Location unavailable"))
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest, callback, Looper.getMainLooper()
            )
            cont.invokeOnCancellation {
                fusedLocationClient.removeLocationUpdates(callback)
            }
        } catch (e: SecurityException) {
            cont.resumeWithException(e)
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocation(): LocationResult2? = suspendCancellableCoroutine { cont ->
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val address = reverseGeocode(location.latitude, location.longitude)
                    cont.resume(LocationResult2(location.latitude, location.longitude, address))
                } else {
                    cont.resume(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e("LocationService", "Failed to get last known location", e)
                cont.resume(null)
            }
    }

    fun reverseGeocode(lat: Double, lon: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                buildString {
                    val parts = listOfNotNull(
                        addr.subThoroughfare,
                        addr.thoroughfare,
                        addr.locality,
                        addr.adminArea,
                        addr.countryName
                    )
                    append(parts.joinToString(", "))
                }
            } else ""
        } catch (e: Exception) {
            Log.e("LocationService", "Geocoding failed: ${e.message}")
            ""
        }
    }
}
