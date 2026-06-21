package dev.echoapp.echo.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import dev.echoapp.echo.utils.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single DataStore instance for the app's preferences. Declared as a top-level
 * Context extension (the library-recommended pattern) so DataStore is created
 * here rather than through the Hilt graph — keeping Dagger from reading
 * DataStore's Kotlin 2.1 metadata (which the Hilt processor can't yet parse).
 */
private val Context.echoDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "echo_prefs"
)

/**
 * Local, device-only user preferences backed by Jetpack DataStore.
 * (Appearance + notification toggles — not synced to the account.)
 */
@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val dataStore: DataStore<Preferences> = context.echoDataStore

    private object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val POIS_LAST_SYNC = longPreferencesKey("pois_last_sync")
        val MAP_MARKER_FILTERS = stringSetPreferencesKey("map_marker_filters")
    }

    private val prefs: Flow<Preferences> = dataStore.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    /** Dark theme on/off (default off — the app is light-first). */
    val darkMode: Flow<Boolean> = prefs.map { it[Keys.DARK_MODE] ?: false }

    /** Whether the user wants notifications (stored for when FCM lands). */
    val notificationsEnabled: Flow<Boolean> = prefs.map { it[Keys.NOTIFICATIONS] ?: true }

    /**
     * Epoch-millis of the last successful POI server sync (0 if never). Lets the POI
     * repo serve the local Firestore cache for free and only re-read from the server
     * once the TTL elapses — POIs are admin-curated reference data that rarely change.
     */
    val poisLastSync: Flow<Long> = prefs.map { it[Keys.POIS_LAST_SYNC] ?: 0L }

    /**
     * The map's active marker-type filters, persisted so the user's chosen view
     * (e.g. parks only) survives app restarts. Defaults to everything shown.
     */
    val mapMarkerFilters: Flow<Set<String>> =
        prefs.map { it[Keys.MAP_MARKER_FILTERS] ?: Constants.DEFAULT_MAP_FILTERS }

    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { it[Keys.DARK_MODE] = enabled }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.NOTIFICATIONS] = enabled }
    }

    suspend fun setPoisLastSync(epochMillis: Long) {
        dataStore.edit { it[Keys.POIS_LAST_SYNC] = epochMillis }
    }

    suspend fun setMapMarkerFilters(filters: Set<String>) {
        dataStore.edit { it[Keys.MAP_MARKER_FILTERS] = filters }
    }
}
