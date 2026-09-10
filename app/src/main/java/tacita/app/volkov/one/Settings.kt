package tacita.app.volkov.one

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

data class AppSettings(
    val enabled: Boolean = true,
    val targetVolume: Int = 0,
    val includeBluetooth: Boolean = false,
)

object SettingsRepository {
    private val enabledKey = booleanPreferencesKey("enabled")
    private val targetVolumeKey = intPreferencesKey("target_volume")
    private val includeBluetoothKey = booleanPreferencesKey("include_bluetooth")

    fun settings(context: Context): Flow<AppSettings> = context.dataStore.data.map { preferences ->
        AppSettings(
            enabled = preferences[enabledKey] ?: true,
            targetVolume = preferences[targetVolumeKey] ?: 0,
            includeBluetooth = preferences[includeBluetoothKey] ?: false,
        )
    }

    suspend fun updateEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[enabledKey] = enabled }
    }

    suspend fun updateTargetVolume(context: Context, targetVolume: Int) {
        context.dataStore.edit { it[targetVolumeKey] = targetVolume.coerceAtLeast(0) }
    }

    suspend fun updateIncludeBluetooth(context: Context, includeBluetooth: Boolean) {
        context.dataStore.edit { it[includeBluetoothKey] = includeBluetooth }
    }
}
