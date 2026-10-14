package com.meettrack.app.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class DeviceLocation(
    val latitude: Double,
    val longitude: Double,
    val formattedAddress: String = ""
)

@Singleton
class LocationService @Inject constructor(
    @ApplicationContext
    private val context: Context
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): DeviceLocation {
        val location = suspendCancellableCoroutine<Location> { cont ->
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
                        cont.resume(location)
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
        val address = reverseGeocode(location.latitude, location.longitude)
        return DeviceLocation(location.latitude, location.longitude, address)
    }

    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocation(): DeviceLocation? {
        val location = suspendCancellableCoroutine<Location?> { cont ->
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (cont.isActive) cont.resume(location)
                }
                .addOnFailureListener { e ->
                    Log.e("LocationService", "Failed to get last known location", e)
                    if (cont.isActive) cont.resume(null)
                }
        } ?: return null

        val address = reverseGeocode(location.latitude, location.longitude)
        return DeviceLocation(location.latitude, location.longitude, address)
    }

    suspend fun reverseGeocode(lat: Double, lon: Double): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                try {
                    Geocoder(context, Locale.getDefault()).getFromLocation(
                        lat,
                        lon,
                        1,
                        object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<android.location.Address>) {
                                if (continuation.isActive) {
                                    continuation.resume(formatAddress(addresses.firstOrNull()))
                                }
                            }

                            override fun onError(errorMessage: String?) {
                                Log.e(
                                    "LocationService",
                                    "Geocoding failed: ${errorMessage ?: "unknown error"}"
                                )
                                if (continuation.isActive) continuation.resume("")
                            }
                        }
                    )
                } catch (e: Exception) {
                    Log.e("LocationService", "Geocoding request failed", e)
                    if (continuation.isActive) continuation.resume("")
                }
            }
        } else {
            withContext(Dispatchers.IO) {
                try {
                    @Suppress("DEPRECATION")
                    val addresses = Geocoder(context, Locale.getDefault())
                        .getFromLocation(lat, lon, 1)
                    formatAddress(addresses?.firstOrNull())
                } catch (e: Exception) {
                    Log.e("LocationService", "Geocoding failed", e)
                    ""
                }
            }
        }

    private fun formatAddress(address: android.location.Address?): String =
        address?.let {
            listOfNotNull(
                it.subThoroughfare,
                it.thoroughfare,
                it.locality,
                it.adminArea,
                it.countryName
            ).joinToString(", ")
        }.orEmpty()
}
