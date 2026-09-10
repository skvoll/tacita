package one.volkov.apps.tacita

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "settings")

data class AppSettings(
    val enabled: Boolean = false,
    val targetPercent: Int = 5,
    val includeBluetooth: Boolean = false,
    val allowVolumeAbove20: Boolean = false,
)

object SettingsRepository {
    private val enabledKey = booleanPreferencesKey("enabled")
    private val targetPercentKey = intPreferencesKey("target_percent")
    private val includeBluetoothKey = booleanPreferencesKey("include_bluetooth")
    private val allowVolumeAbove20Key = booleanPreferencesKey("allow_volume_above_20")

    fun settings(context: Context): Flow<AppSettings> = context.dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences ->
            val allowVolumeAbove20 = preferences[allowVolumeAbove20Key] ?: false
            AppSettings(
                enabled = preferences[enabledKey] ?: false,
                targetPercent = (preferences[targetPercentKey] ?: 5).coerceIn(
                    0,
                    if (allowVolumeAbove20) 100 else 20,
                ),
                includeBluetooth = preferences[includeBluetoothKey] ?: false,
                allowVolumeAbove20 = allowVolumeAbove20,
            )
        }

    suspend fun updateEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[enabledKey] = enabled }
    }

    suspend fun updateTargetPercent(context: Context, targetPercent: Int) {
        context.dataStore.edit { preferences ->
            val maximum = if (preferences[allowVolumeAbove20Key] == true) 100 else 20
            preferences[targetPercentKey] = targetPercent.coerceIn(0, maximum)
        }
    }

    suspend fun updateIncludeBluetooth(context: Context, includeBluetooth: Boolean) {
        context.dataStore.edit { it[includeBluetoothKey] = includeBluetooth }
    }

    suspend fun updateAllowVolumeAbove20(context: Context, allowed: Boolean) {
        context.dataStore.edit {
            it[allowVolumeAbove20Key] = allowed
            if (!allowed && (it[targetPercentKey] ?: 5) > 20) it[targetPercentKey] = 20
        }
    }
}
