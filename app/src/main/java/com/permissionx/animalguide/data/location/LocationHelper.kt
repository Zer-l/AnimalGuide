package com.permissionx.animalguide.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
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

            // 先尝试缓存（GPS 精度更高，优先读）
            val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            val location = lastLocation ?: run {
                // 主动请求用 Network（速度快，室内也能用）
                val requestProvider = if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
                    LocationManager.NETWORK_PROVIDER
                } else {
                    LocationManager.GPS_PROVIDER
                }

                withTimeoutOrNull(5000) {
                    suspendCancellableCoroutine { cont ->
                        val listener = object : LocationListener {
                            override fun onLocationChanged(loc: Location) {
                                locationManager.removeUpdates(this)
                                if (cont.isActive) cont.resume(loc)
                            }

                            @Deprecated("Deprecated in Java")
                            override fun onStatusChanged(
                                provider: String?,
                                status: Int,
                                extras: android.os.Bundle?
                            ) {
                            }

                            override fun onProviderEnabled(provider: String) {}
                            override fun onProviderDisabled(provider: String) {
                                if (cont.isActive) cont.resume(null)
                            }
                        }

                        Handler(Looper.getMainLooper()).post {
                            try {
                                locationManager.requestLocationUpdates(
                                    requestProvider,
                                    0L,
                                    0f,
                                    listener
                                )
                            } catch (e: Exception) {
                                if (cont.isActive) cont.resume(null)
                            }
                        }

                        cont.invokeOnCancellation {
                            locationManager.removeUpdates(listener)
                        }
                    }
                }
            }

            location?.let {
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