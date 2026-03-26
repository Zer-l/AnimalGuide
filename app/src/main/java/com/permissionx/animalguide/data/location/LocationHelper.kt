package com.permissionx.animalguide.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

data class LocationResult(
    val latitude: Double,
    val longitude: Double,
    val address: String = ""
)

@Singleton
class LocationHelper @Inject constructor() {

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): LocationResult? {
        return try {
            val locationManager =
                context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER
            ).filter { locationManager.isProviderEnabled(it) }

            if (providers.isEmpty()) return null

            val provider = if (providers.contains(LocationManager.GPS_PROVIDER)) {
                LocationManager.GPS_PROVIDER
            } else {
                LocationManager.NETWORK_PROVIDER
            }

            val lastLocation = locationManager.getLastKnownLocation(provider)

            val location = lastLocation ?: run {
                suspendCancellableCoroutine { cont ->
                    val listener = object : android.location.LocationListener {
                        override fun onLocationChanged(loc: android.location.Location) {
                            locationManager.removeUpdates(this)
                            cont.resume(loc)
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onStatusChanged(
                            provider: String?,
                            status: Int,
                            extras: android.os.Bundle?
                        ) {
                        }
                    }
                    locationManager.requestLocationUpdates(provider, 0L, 0f, listener)
                    cont.invokeOnCancellation { locationManager.removeUpdates(listener) }
                }
            }

            location.let {
                val address = getAddress(context, it.latitude, it.longitude)
                LocationResult(it.latitude, it.longitude, address)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun formatAddress(addr: android.location.Address): String {
        return listOfNotNull(
            addr.adminArea,
            addr.subAdminArea,
            addr.locality,
            addr.subLocality,
            addr.thoroughfare,
            addr.subThoroughfare
        ).distinct().joinToString("")
    }

    suspend fun getAddress(
        context: Context,
        latitude: Double,
        longitude: Double
    ): String {
        return try {
            val geocoder = Geocoder(context, Locale.CHINA)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        val address = addresses.firstOrNull()?.let { formatAddress(it) } ?: ""
                        cont.resume(address)
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                addresses?.firstOrNull()?.let { formatAddress(it) } ?: ""
            }
        } catch (_: Exception) {
            ""
        }
    }
}