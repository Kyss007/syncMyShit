package com.syncmyshit.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.syncmyshit.app.data.model.EmulatorProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "syncmyshit_prefs")

class PreferencesManager(private val context: Context) {

    companion object {
        val KEY_GOOGLE_ACCOUNT_EMAIL = stringPreferencesKey("google_account_email")
        val KEY_DRIVE_ROOT_FOLDER_ID = stringPreferencesKey("drive_root_folder_id")
        val KEY_AUTO_SYNC_ENABLED = booleanPreferencesKey("auto_sync_enabled")
        val KEY_DETECT_APP_SWITCH = booleanPreferencesKey("detect_app_switch")
        val KEY_WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val KEY_SYNC_INTERVAL_MINUTES = intPreferencesKey("sync_interval_minutes")
        val KEY_KEEP_BACKUPS_COUNT = intPreferencesKey("keep_backups_count")
        val KEY_IS_SETUP_COMPLETED = booleanPreferencesKey("is_setup_completed")
        val KEY_CUSTOM_PROFILES_JSON = stringPreferencesKey("custom_profiles_json")
        val KEY_CUSTOM_OAUTH_CLIENT_ID = stringPreferencesKey("custom_oauth_client_id")
        val KEY_CUSTOM_OAUTH_CLIENT_SECRET = stringPreferencesKey("custom_oauth_client_secret")
        val KEY_LAST_SYNC_TIMESTAMP = stringPreferencesKey("last_sync_timestamp")
        val KEY_OAUTH_ACCESS_TOKEN = stringPreferencesKey("oauth_access_token")
        val KEY_OAUTH_REFRESH_TOKEN = stringPreferencesKey("oauth_refresh_token")
        val KEY_OAUTH_EXPIRES_AT = stringPreferencesKey("oauth_expires_at")
        val KEY_OAUTH_CODE_VERIFIER = stringPreferencesKey("oauth_code_verifier")
        val KEY_OAUTH_STATE = stringPreferencesKey("oauth_state")
    }

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    val isSetupCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_IS_SETUP_COMPLETED] ?: false
    }

    val googleAccountEmail: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_GOOGLE_ACCOUNT_EMAIL]
    }

    val driveRootFolderId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_DRIVE_ROOT_FOLDER_ID]
    }

    val autoSyncEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_SYNC_ENABLED] ?: true
    }

    val detectAppSwitch: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_DETECT_APP_SWITCH] ?: true
    }

    val wifiOnly: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_WIFI_ONLY] ?: false
    }

    val syncIntervalMinutes: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_SYNC_INTERVAL_MINUTES] ?: 30
    }

    val keepBackupsCount: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_KEEP_BACKUPS_COUNT] ?: 5
    }

    val customOAuthClientId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_CUSTOM_OAUTH_CLIENT_ID] ?: ""
    }

    val customOAuthClientSecret: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_CUSTOM_OAUTH_CLIENT_SECRET] ?: ""
    }

    val lastSyncTimestamp: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_SYNC_TIMESTAMP]?.toLongOrNull() ?: 0L
    }

    val customProfiles: Flow<List<EmulatorProfile>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_CUSTOM_PROFILES_JSON] ?: "[]"
        runCatching {
            json.decodeFromString<List<EmulatorProfile>>(raw)
        }.getOrDefault(emptyList())
    }

    suspend fun setSetupCompleted(completed: Boolean) {
        context.dataStore.edit { it[KEY_IS_SETUP_COMPLETED] = completed }
    }

    suspend fun setGoogleAccount(email: String?, driveFolderId: String? = null) {
        context.dataStore.edit { prefs ->
            if (email != null) {
                prefs[KEY_GOOGLE_ACCOUNT_EMAIL] = email
            } else {
                prefs.remove(KEY_GOOGLE_ACCOUNT_EMAIL)
            }
            if (driveFolderId != null) {
                prefs[KEY_DRIVE_ROOT_FOLDER_ID] = driveFolderId
            }
        }
    }

    suspend fun setDriveRootFolderId(folderId: String) {
        context.dataStore.edit { it[KEY_DRIVE_ROOT_FOLDER_ID] = folderId }
    }

    suspend fun setAutoSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_SYNC_ENABLED] = enabled }
    }

    suspend fun setDetectAppSwitch(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DETECT_APP_SWITCH] = enabled }
    }

    suspend fun setWifiOnly(wifiOnly: Boolean) {
        context.dataStore.edit { it[KEY_WIFI_ONLY] = wifiOnly }
    }

    suspend fun setSyncIntervalMinutes(minutes: Int) {
        context.dataStore.edit { it[KEY_SYNC_INTERVAL_MINUTES] = minutes }
    }

    suspend fun setKeepBackupsCount(count: Int) {
        context.dataStore.edit { it[KEY_KEEP_BACKUPS_COUNT] = count }
    }

    suspend fun setCustomOAuthCredentials(clientId: String, clientSecret: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CUSTOM_OAUTH_CLIENT_ID] = clientId
            prefs[KEY_CUSTOM_OAUTH_CLIENT_SECRET] = clientSecret
        }
    }

    suspend fun updateLastSyncTimestamp(timestamp: Long = System.currentTimeMillis()) {
        context.dataStore.edit { it[KEY_LAST_SYNC_TIMESTAMP] = timestamp.toString() }
    }

    suspend fun saveCustomProfiles(profiles: List<EmulatorProfile>) {
        val raw = json.encodeToString(profiles)
        context.dataStore.edit { it[KEY_CUSTOM_PROFILES_JSON] = raw }
    }

    suspend fun addCustomProfile(profile: EmulatorProfile) {
        context.dataStore.edit { prefs ->
            val raw = prefs[KEY_CUSTOM_PROFILES_JSON] ?: "[]"
            val current = runCatching {
                json.decodeFromString<List<EmulatorProfile>>(raw)
            }.getOrDefault(emptyList()).toMutableList()

            current.removeAll { it.id == profile.id }
            current.add(profile)
            prefs[KEY_CUSTOM_PROFILES_JSON] = json.encodeToString(current)
        }
    }

    suspend fun removeCustomProfile(profileId: String) {
        context.dataStore.edit { prefs ->
            val raw = prefs[KEY_CUSTOM_PROFILES_JSON] ?: "[]"
            val current = runCatching {
                json.decodeFromString<List<EmulatorProfile>>(raw)
            }.getOrDefault(emptyList()).toMutableList()

            current.removeAll { it.id == profileId }
            prefs[KEY_CUSTOM_PROFILES_JSON] = json.encodeToString(current)
        }
    }

    val oauthAccessToken: Flow<String?> = context.dataStore.data.map { it[KEY_OAUTH_ACCESS_TOKEN] }
    val oauthRefreshToken: Flow<String?> = context.dataStore.data.map { it[KEY_OAUTH_REFRESH_TOKEN] }
    val oauthCodeVerifier: Flow<String?> = context.dataStore.data.map { it[KEY_OAUTH_CODE_VERIFIER] }
    val oauthState: Flow<String?> = context.dataStore.data.map { it[KEY_OAUTH_STATE] }

    suspend fun saveOAuthTokens(accessToken: String, refreshToken: String?, expiresInSeconds: Long) {
        val expiresAt = System.currentTimeMillis() + (expiresInSeconds * 1000L)
        context.dataStore.edit { prefs ->
            prefs[KEY_OAUTH_ACCESS_TOKEN] = accessToken
            if (refreshToken != null) {
                prefs[KEY_OAUTH_REFRESH_TOKEN] = refreshToken
            }
            prefs[KEY_OAUTH_EXPIRES_AT] = expiresAt.toString()
        }
    }

    suspend fun saveOAuthPkceSession(verifier: String, state: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_OAUTH_CODE_VERIFIER] = verifier
            prefs[KEY_OAUTH_STATE] = state
        }
    }

    suspend fun clearOAuthPkceSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_OAUTH_CODE_VERIFIER)
            prefs.remove(KEY_OAUTH_STATE)
        }
    }

    suspend fun clearOAuthTokens() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_OAUTH_ACCESS_TOKEN)
            prefs.remove(KEY_OAUTH_REFRESH_TOKEN)
            prefs.remove(KEY_OAUTH_EXPIRES_AT)
            prefs.remove(KEY_GOOGLE_ACCOUNT_EMAIL)
        }
    }
}
