package com.example.ai

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File

class MediaPipeLlmEngine : LlmEngine {

    override val engineName: String = "MediaPipe On-Device LLM"
    private var llmInference: LlmInference? = null
    private var _isReady: Boolean = false
    override val isReady: Boolean get() = _isReady

    private var _status: EngineStatus = EngineStatus.NotReady
    override val status: EngineStatus get() = _status

    private var isCancelled = false

    override suspend fun initialize(
        context: Context,
        modelFile: File?,
        config: InferenceConfig
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (modelFile == null || !modelFile.exists() || modelFile.length() < 1024 * 1024) {
            _status = EngineStatus.Error("মডেল ফাইলটি পাওয়া যায়নি বা ক্ষতিগ্রস্ত হয়েছে।")
            return@withContext Result.failure(IllegalStateException("Invalid model file"))
        }

        try {
            _status = EngineStatus.Loading("মডেল মেমোরিতে লোড হচ্ছে...")

            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(config.maxTokens)
                .setResultListener { partialResult, done ->
                    // Callback if async streaming is used
                }
                .setErrorListener { error ->
                    _status = EngineStatus.Error("ইনফারেন্স ত্রুটি: ${error.message}")
                }
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            _isReady = true
            _status = EngineStatus.Ready(
                modelName = modelFile.nameWithoutExtension,
                isHardwareAccelerated = true
            )
            Result.success(Unit)
        } catch (e: Exception) {
            _isReady = false
            _status = EngineStatus.Error("মডেল লোড ব্যর্থ: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    override fun generateStream(
        prompt: String,
        contextHistory: List<Pair<String, String>>,
        config: InferenceConfig
    ): Flow<String> = flow {
        val inference = llmInference ?: throw IllegalStateException("Model is not initialized")
        isCancelled = false

        // Build prompt with context history
        val fullPrompt = buildString {
            if (config.systemPrompt.isNotBlank()) {
                appendLine("System: ${config.systemPrompt}")
            }
            contextHistory.takeLast(6).forEach { (role, content) ->
                val speaker = if (role == "user") "User" else "Assistant"
                appendLine("$speaker: $content")
            }
            append("User: $prompt\nAssistant: ")
        }

        try {
            val response = inference.generateResponse(fullPrompt)
            // Stream chunks
            val words = response.split(Regex("(?<=\\s)|(?<=\\n)"))
            for (word in words) {
                if (isCancelled) break
                emit(word)
                kotlinx.coroutines.delay(16)
            }
        } catch (e: CancellationException) {
            isCancelled = true
        } catch (e: Exception) {
            emit("\n[ত্রুটি: ${e.localizedMessage ?: "ইনফারেন্সে সমস্যা দেখা দিয়েছে"}]")
        }
    }

    override fun stop() {
        isCancelled = true
    }

    override fun release() {
        try {
            llmInference = null
            _isReady = false
            _status = EngineStatus.NotReady
        } catch (_: Exception) {}
    }
}
