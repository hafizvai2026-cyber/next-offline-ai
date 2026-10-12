package com.example.ai

import android.content.Context
import kotlinx.coroutines.flow.Flow
import java.io.File

data class InferenceConfig(
    val temperature: Float = 0.7f,
    val maxTokens: Int = 512,
    val topK: Int = 40,
    val randomSeed: Int = 42,
    val systemPrompt: String = "",
    val performancePreset: String = "balanced"
)

data class ModelDescriptor(
    val id: String,
    val name: String,
    val description: String,
    val parameterSize: String,
    val downloadUrl: String?,
    val expectedSizeMb: Long,
    val fileName: String,
    val format: String, // "EMBEDDED", "TASK", "BIN", "GGUF"
    val isBuiltIn: Boolean = false,
    val license: String = "Open Weights",
    val recommendedRam: String = "2GB+"
)

sealed class EngineStatus {
    object NotReady : EngineStatus()
    data class Loading(val message: String) : EngineStatus()
    data class Ready(val modelName: String, val isHardwareAccelerated: Boolean) : EngineStatus()
    data class Error(val message: String) : EngineStatus()
}

interface LlmEngine {
    val engineName: String
    val isReady: Boolean
    val status: EngineStatus

    suspend fun initialize(context: Context, modelFile: File?, config: InferenceConfig): Result<Unit>
    fun generateStream(prompt: String, contextHistory: List<Pair<String, String>>, config: InferenceConfig): Flow<String>
    fun stop()
    fun release()
}
