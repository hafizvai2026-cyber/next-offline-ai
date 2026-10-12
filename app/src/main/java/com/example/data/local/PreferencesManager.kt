package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class PreferencesManager(private val context: Context) {

    companion object {
        val KEY_LANGUAGE = stringPreferencesKey("pref_language")
        val KEY_RESPONSE_LENGTH = stringPreferencesKey("pref_response_length")
        val KEY_TEMPERATURE = floatPreferencesKey("pref_temperature")
        val KEY_MAX_TOKENS = intPreferencesKey("pref_max_tokens")
        val KEY_PRESET = stringPreferencesKey("pref_performance_preset")
        val KEY_HAPTIC = booleanPreferencesKey("pref_haptic_enabled")
        val KEY_ENTER_TO_SEND = booleanPreferencesKey("pref_enter_to_send")
        val KEY_CUSTOM_INSTRUCTION = stringPreferencesKey("pref_custom_instruction")
        val KEY_SELECTED_MODEL = stringPreferencesKey("pref_selected_model")
        val KEY_FIRST_LAUNCH = booleanPreferencesKey("pref_first_launch")
    }

    val languageFlow: Flow<String> = context.dataStore.data.map { it[KEY_LANGUAGE] ?: "auto" }
    val responseLengthFlow: Flow<String> = context.dataStore.data.map { it[KEY_RESPONSE_LENGTH] ?: "balanced" }
    val temperatureFlow: Flow<Float> = context.dataStore.data.map { it[KEY_TEMPERATURE] ?: 0.7f }
    val maxTokensFlow: Flow<Int> = context.dataStore.data.map { it[KEY_MAX_TOKENS] ?: 512 }
    val presetFlow: Flow<String> = context.dataStore.data.map { it[KEY_PRESET] ?: "balanced" }
    val hapticFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_HAPTIC] ?: true }
    val enterToSendFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_ENTER_TO_SEND] ?: false }
    val customInstructionFlow: Flow<String> = context.dataStore.data.map {
        it[KEY_CUSTOM_INSTRUCTION] ?: "আপনি Next Offline Chat AI। ইন্টারনেট ছাড়া অন-ডিভাইসে ব্যবহারকারীকে সব ধরনের সাহায্য করুন।"
    }
    val selectedModelFlow: Flow<String> = context.dataStore.data.map { it[KEY_SELECTED_MODEL] ?: "embedded_bengali" }
    val isFirstLaunchFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_FIRST_LAUNCH] ?: true }

    suspend fun setLanguage(lang: String) = context.dataStore.edit { it[KEY_LANGUAGE] = lang }
    suspend fun setResponseLength(length: String) = context.dataStore.edit { it[KEY_RESPONSE_LENGTH] = length }
    suspend fun setTemperature(temp: Float) = context.dataStore.edit { it[KEY_TEMPERATURE] = temp }
    suspend fun setMaxTokens(tokens: Int) = context.dataStore.edit { it[KEY_MAX_TOKENS] = tokens }
    suspend fun setPreset(preset: String) = context.dataStore.edit { it[KEY_PRESET] = preset }
    suspend fun setHaptic(enabled: Boolean) = context.dataStore.edit { it[KEY_HAPTIC] = enabled }
    suspend fun setEnterToSend(enabled: Boolean) = context.dataStore.edit { it[KEY_ENTER_TO_SEND] = enabled }
    suspend fun setCustomInstruction(instruction: String) = context.dataStore.edit { it[KEY_CUSTOM_INSTRUCTION] = instruction }
    suspend fun setSelectedModel(modelId: String) = context.dataStore.edit { it[KEY_SELECTED_MODEL] = modelId }
    suspend fun markFirstLaunchComplete() = context.dataStore.edit { it[KEY_FIRST_LAUNCH] = false }
}
