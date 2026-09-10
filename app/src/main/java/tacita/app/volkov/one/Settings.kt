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
    val targetPercent: Int = 5,
    val includeBluetooth: Boolean = false,
    val allowVolumeAbove20: Boolean = false,
)

object SettingsRepository {
    private val enabledKey = booleanPreferencesKey("enabled")
    private val targetPercentKey = intPreferencesKey("target_percent")
    private val includeBluetoothKey = booleanPreferencesKey("include_bluetooth")
    private val allowVolumeAbove20Key = booleanPreferencesKey("allow_volume_above_20")

    fun settings(context: Context): Flow<AppSettings> = context.dataStore.data.map { preferences ->
        AppSettings(
            enabled = preferences[enabledKey] ?: true,
            targetPercent = preferences[targetPercentKey] ?: 5,
            includeBluetooth = preferences[includeBluetoothKey] ?: false,
            allowVolumeAbove20 = preferences[allowVolumeAbove20Key] ?: false,
        )
    }

    suspend fun updateEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[enabledKey] = enabled }
    }

    suspend fun updateTargetPercent(context: Context, targetPercent: Int) {
        context.dataStore.edit { it[targetPercentKey] = targetPercent.coerceIn(0, 100) }
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
