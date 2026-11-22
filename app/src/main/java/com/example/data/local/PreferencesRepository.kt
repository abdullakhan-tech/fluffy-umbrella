package com.example.data.local

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private const val PREFS_TAG = "PreferencesRepo"

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "zaythar_prefs",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
)

data class ZaytharPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentHex: Long = 0xFFFF3155L,
    val motion3dEnabled: Boolean = true,
    val processingVelocity: Float = 88f,
    val synapticDepth: Float = 76f,
    val responsePrecision: Float = 92f,
    val creativityBias: Float = 68f,
    val neuralOverdrive: Boolean = true,
    val holographicUi: Boolean = true,
    val autoCognition: Boolean = true,
    val glowStrength: Float = 72f,
    val metalSpecularity: Float = 65f,
    val contrastStrength: Float = 80f,
    val empathyRatio: Float = 92f,
    val cognitiveCapacity: Float = 85f,
    val adaptability: Float = 90f,
    val personaCreativity: Float = 88f,
    val personaStyle: String = "Balanced & helpful",
    val selectedProviderRoute: String = "hybrid",
    val languageCode: String = "en-US",
    val speakReplies: Boolean = false,
    val webConnectEnabled: Boolean = true,
    val mapsGroundingEnabled: Boolean = false,
    val operatorName: String = "OPERATOR 01",
    val lastActiveSessionUid: String = "",
    val lastActiveSessionId: String = "",
    val companionBackendUrl: String = "http://10.0.2.2:8000",
    val useCompanionBackend: Boolean = false
)

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val ACCENT_HEX = longPreferencesKey("accent_hex")
        val MOTION_3D = booleanPreferencesKey("motion_3d")
        val PROCESSING_VELOCITY = floatPreferencesKey("processing_velocity")
        val SYNAPTIC_DEPTH = floatPreferencesKey("synaptic_depth")
        val RESPONSE_PRECISION = floatPreferencesKey("response_precision")
        val CREATIVITY_BIAS = floatPreferencesKey("creativity_bias")
        val NEURAL_OVERDRIVE = booleanPreferencesKey("neural_overdrive")
        val HOLOGRAPHIC_UI = booleanPreferencesKey("holographic_ui")
        val AUTO_COGNITION = booleanPreferencesKey("auto_cognition")
        val GLOW_STRENGTH = floatPreferencesKey("glow_strength")
        val METAL_SPECULARITY = floatPreferencesKey("metal_specularity")
        val CONTRAST_STRENGTH = floatPreferencesKey("contrast_strength")
        val EMPATHY_RATIO = floatPreferencesKey("empathy_ratio")
        val COGNITIVE_CAPACITY = floatPreferencesKey("cognitive_capacity")
        val ADAPTABILITY = floatPreferencesKey("adaptability")
        val PERSONA_CREATIVITY = floatPreferencesKey("persona_creativity")
        val PERSONA_STYLE = stringPreferencesKey("persona_style")
        val PROVIDER_ROUTE = stringPreferencesKey("provider_route")
        val LANGUAGE_CODE = stringPreferencesKey("language_code")
        val SPEAK_REPLIES = booleanPreferencesKey("speak_replies")
        val WEB_CONNECT = booleanPreferencesKey("web_connect")
        val MAPS_GROUNDING = booleanPreferencesKey("maps_grounding")
        val OPERATOR_NAME = stringPreferencesKey("operator_name")
        val LAST_ACTIVE_SESSION_UID = stringPreferencesKey("last_active_session_uid")
        val LAST_ACTIVE_SESSION_ID = stringPreferencesKey("last_active_session_id")
        val COMPANION_BACKEND_URL = stringPreferencesKey("companion_backend_url")
        val USE_COMPANION_BACKEND = booleanPreferencesKey("use_companion_backend")
    }

    private fun mapPreferences(prefs: Preferences): ZaytharPreferences {
        val resolvedThemeMode = prefs[Keys.THEME_MODE]?.let { ThemeMode.fromId(it) }
            ?: when (prefs[Keys.DARK_THEME]) {
                false -> ThemeMode.LIGHT
                true -> ThemeMode.DARK
                null -> ThemeMode.SYSTEM
            }
        return ZaytharPreferences(
            themeMode = resolvedThemeMode,
            accentHex = prefs[Keys.ACCENT_HEX] ?: 0xFFFF3155L,
            motion3dEnabled = prefs[Keys.MOTION_3D] ?: true,
            processingVelocity = prefs[Keys.PROCESSING_VELOCITY] ?: 88f,
            synapticDepth = prefs[Keys.SYNAPTIC_DEPTH] ?: 76f,
            responsePrecision = prefs[Keys.RESPONSE_PRECISION] ?: 92f,
            creativityBias = prefs[Keys.CREATIVITY_BIAS] ?: 68f,
            neuralOverdrive = prefs[Keys.NEURAL_OVERDRIVE] ?: true,
            holographicUi = prefs[Keys.HOLOGRAPHIC_UI] ?: true,
            autoCognition = prefs[Keys.AUTO_COGNITION] ?: true,
            glowStrength = prefs[Keys.GLOW_STRENGTH] ?: 72f,
            metalSpecularity = prefs[Keys.METAL_SPECULARITY] ?: 65f,
            contrastStrength = prefs[Keys.CONTRAST_STRENGTH] ?: 80f,
            empathyRatio = prefs[Keys.EMPATHY_RATIO] ?: 92f,
            cognitiveCapacity = prefs[Keys.COGNITIVE_CAPACITY] ?: 85f,
            adaptability = prefs[Keys.ADAPTABILITY] ?: 90f,
            personaCreativity = prefs[Keys.PERSONA_CREATIVITY] ?: 88f,
            personaStyle = prefs[Keys.PERSONA_STYLE] ?: "Balanced & helpful",
            selectedProviderRoute = prefs[Keys.PROVIDER_ROUTE] ?: "hybrid",
            languageCode = prefs[Keys.LANGUAGE_CODE] ?: "en-US",
            speakReplies = prefs[Keys.SPEAK_REPLIES] ?: false,
            webConnectEnabled = prefs[Keys.WEB_CONNECT] ?: true,
            mapsGroundingEnabled = prefs[Keys.MAPS_GROUNDING] ?: false,
            operatorName = prefs[Keys.OPERATOR_NAME] ?: "OPERATOR 01",
            lastActiveSessionUid = prefs[Keys.LAST_ACTIVE_SESSION_UID] ?: "",
            lastActiveSessionId = prefs[Keys.LAST_ACTIVE_SESSION_ID] ?: "",
            companionBackendUrl = prefs[Keys.COMPANION_BACKEND_URL] ?: "http://10.0.2.2:8000",
            useCompanionBackend = prefs[Keys.USE_COMPANION_BACKEND] ?: false
        )
    }

    val preferences: Flow<ZaytharPreferences> = context.dataStore.data
        .catch { error ->
            if (error is IOException) {
                Log.w(PREFS_TAG, "DataStore read error; emitting defaults", error)
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map(::mapPreferences)

    suspend fun setLastActiveSession(userId: String, sessionId: String) {
        update {
            it.copy(
                lastActiveSessionUid = userId,
                lastActiveSessionId = sessionId
            )
        }
    }

    suspend fun update(transform: (ZaytharPreferences) -> ZaytharPreferences) {
        try {
            context.dataStore.edit { prefs ->
                val current = mapPreferences(prefs)
                val next = transform(current)
                prefs[Keys.THEME_MODE] = next.themeMode.id
                prefs[Keys.DARK_THEME] = next.themeMode == ThemeMode.DARK
                prefs[Keys.ACCENT_HEX] = next.accentHex
                prefs[Keys.MOTION_3D] = next.motion3dEnabled
                prefs[Keys.PROCESSING_VELOCITY] = next.processingVelocity
                prefs[Keys.SYNAPTIC_DEPTH] = next.synapticDepth
                prefs[Keys.RESPONSE_PRECISION] = next.responsePrecision
                prefs[Keys.CREATIVITY_BIAS] = next.creativityBias
                prefs[Keys.NEURAL_OVERDRIVE] = next.neuralOverdrive
                prefs[Keys.HOLOGRAPHIC_UI] = next.holographicUi
                prefs[Keys.AUTO_COGNITION] = next.autoCognition
                prefs[Keys.GLOW_STRENGTH] = next.glowStrength
                prefs[Keys.METAL_SPECULARITY] = next.metalSpecularity
                prefs[Keys.CONTRAST_STRENGTH] = next.contrastStrength
                prefs[Keys.EMPATHY_RATIO] = next.empathyRatio
                prefs[Keys.COGNITIVE_CAPACITY] = next.cognitiveCapacity
                prefs[Keys.ADAPTABILITY] = next.adaptability
                prefs[Keys.PERSONA_CREATIVITY] = next.personaCreativity
                prefs[Keys.PERSONA_STYLE] = next.personaStyle
                prefs[Keys.PROVIDER_ROUTE] = next.selectedProviderRoute
                prefs[Keys.LANGUAGE_CODE] = next.languageCode
                prefs[Keys.SPEAK_REPLIES] = next.speakReplies
                prefs[Keys.WEB_CONNECT] = next.webConnectEnabled
                prefs[Keys.MAPS_GROUNDING] = next.mapsGroundingEnabled
                prefs[Keys.OPERATOR_NAME] = next.operatorName
                prefs[Keys.LAST_ACTIVE_SESSION_UID] = next.lastActiveSessionUid
                prefs[Keys.LAST_ACTIVE_SESSION_ID] = next.lastActiveSessionId
                prefs[Keys.COMPANION_BACKEND_URL] = next.companionBackendUrl
                prefs[Keys.USE_COMPANION_BACKEND] = next.useCompanionBackend
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(PREFS_TAG, "Failed to persist DataStore update", e)
        }
    }
}
