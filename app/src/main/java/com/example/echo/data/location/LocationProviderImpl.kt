package com.example.echo.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.echo.di.IoDispatcher
import com.example.echo.domain.model.Coordinates
import com.example.echo.domain.repository.LocationProvider
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import android.location.Address
import android.location.Geocoder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * [LocationProvider] backed by Google Play Services' fused location provider.
 */
@Singleton
class LocationProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : LocationProvider {

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    override suspend fun getCurrentCoordinates(): Coordinates? = withContext(ioDispatcher) {
        if (!hasLocationPermission()) return@withContext null

        try {
            val location = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).await() ?: fusedLocationClient.lastLocation.await()

            location?.let { Coordinates(it.latitude, it.longitude) }
        } catch (e: SecurityException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getNeighborhoodName(coordinates: Coordinates): String? = withContext(ioDispatcher) {
        if (!Geocoder.isPresent()) return@withContext null

        val geocoder = Geocoder(context, Locale.getDefault())
        val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine<List<Address>?> { continuation ->
                try {
                    geocoder.getFromLocation(
                        coordinates.latitude,
                        coordinates.longitude,
                        1,
                        object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: List<Address>) {
                                continuation.resume(addresses)
                            }
                            override fun onError(errorMessage: String?) {
                                continuation.resume(null)
                            }
                        }
                    )
                } catch (e: Exception) {
                    continuation.resume(null)
                }
            }
        } else {
            try {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(coordinates.latitude, coordinates.longitude, 1)
            } catch (e: Exception) {
                null
            }
        }

        val address = addresses?.firstOrNull() ?: return@withContext null
        
        val name = address.subLocality
            ?: address.locality
            ?: address.subAdminArea
            ?: address.adminArea

        if (!name.isNullOrBlank()) name else null
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }
}
