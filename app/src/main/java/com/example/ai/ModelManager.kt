package com.example.ai

import android.content.Context
import android.os.Environment
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

data class DownloadProgress(
    val modelId: String,
    val isDownloading: Boolean = false,
    val progressPercent: Int = 0,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val speedKbps: Long = 0L,
    val errorMessage: String? = null
)

class ModelManager(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val _downloadProgress = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, DownloadProgress>> = _downloadProgress.asStateFlow()

    private val modelsDir: File by lazy {
        val dir = File(context.filesDir, "models")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    val availableModels: List<ModelDescriptor> = listOf(
        ModelDescriptor(
            id = "embedded_bengali",
            name = "Next On-Device Linguistic AI",
            description = "ডিভাইসের সাথেই সমন্বিত। ডাউনলোড ছাড়াই ১০০% অফলাইনে বাংলা ও ইংরেজি লেখালেখি, কোডিং ও গণিতে পারদর্শী।",
            parameterSize = "Integrated",
            downloadUrl = null,
            expectedSizeMb = 0,
            fileName = "embedded",
            format = "EMBEDDED",
            isBuiltIn = true,
            license = "Proprietary Next Graphics",
            recommendedRam = "যেকোনো অ্যান্ড্রয়েড ডিভাইস"
        ),
        ModelDescriptor(
            id = "gemma_2b_it",
            name = "Gemma 2B IT (MediaPipe 4-bit)",
            description = "গুগলের ওপেন-ওয়েট ইন্সট্রাকশন-টিউনড মডেল। বাংলা ও ইংরেজিতে উচ্চমানের ব্যাকরণ ও যুক্তি প্রয়োগে সক্ষম।",
            parameterSize = "2.0B Int4",
            downloadUrl = "https://huggingface.co/google/gemma-2b-it/resolve/main/gemma-2b-it-cpu-int4.bin",
            expectedSizeMb = 1350,
            fileName = "gemma-2b-it-cpu-int4.bin",
            format = "BIN",
            isBuiltIn = false,
            license = "Gemma Terms of Use",
            recommendedRam = "৪ জিবি বা ততোধিক র‍্যাম"
        ),
        ModelDescriptor(
            id = "smollm_1.7b",
            name = "SmolLM 1.7B Multilingual",
            description = "হালকা ও দ্রুতগতির ওপেন মডেল। কম মেমোরি খরচে স্মার্ট উত্তর প্রদান করে।",
            parameterSize = "1.7B Int4",
            downloadUrl = "https://huggingface.co/HuggingFaceTB/SmolLM-1.7B-Instruct/resolve/main/model.bin",
            expectedSizeMb = 980,
            fileName = "smollm-1.7b-instruct.bin",
            format = "BIN",
            isBuiltIn = false,
            license = "Apache 2.0",
            recommendedRam = "৩ জিবি বা ততোধিক র‍্যাম"
        ),
        ModelDescriptor(
            id = "tinyllama_1.1b",
            name = "TinyLlama 1.1B Chat",
            description = "কম্প্যাক্ট ৪-বিট নিউরাল মডেল। সাধারণ চ্যাট ও দৈনন্দিন অফলাইন প্রশ্নোত্তরে চমৎকার।",
            parameterSize = "1.1B Q4",
            downloadUrl = "https://huggingface.co/TinyLlama/TinyLlama-1.1B-Chat-v1.0/resolve/main/model.bin",
            expectedSizeMb = 680,
            fileName = "tinyllama-1.1b-chat.bin",
            format = "BIN",
            isBuiltIn = false,
            license = "Apache 2.0",
            recommendedRam = "২ জিবি বা ততোধিক র‍্যাম"
        )
    )

    fun getModelFile(descriptor: ModelDescriptor): File {
        return File(modelsDir, descriptor.fileName)
    }

    fun isModelInstalled(descriptor: ModelDescriptor): Boolean {
        if (descriptor.isBuiltIn) return true
        val file = getModelFile(descriptor)
        return file.exists() && file.length() > 10 * 1024 * 1024 // At least 10MB
    }

    fun getAvailableStorageMb(): Long {
        return try {
            val stat = StatFs(context.filesDir.absolutePath)
            (stat.availableBlocksLong * stat.blockSizeLong) / (1024 * 1024)
        } catch (_: Exception) {
            1024L
        }
    }

    fun getUsedStorageMb(): Long {
        return try {
            val totalBytes = modelsDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
            totalBytes / (1024 * 1024)
        } catch (_: Exception) {
            0L
        }
    }

    suspend fun downloadModel(descriptor: ModelDescriptor): Result<Unit> = withContext(Dispatchers.IO) {
        val url = descriptor.downloadUrl ?: return@withContext Result.failure(IllegalArgumentException("No download URL"))
        val targetFile = getModelFile(descriptor)
        val tempFile = File(modelsDir, "${descriptor.fileName}.tmp")

        if (getAvailableStorageMb() < descriptor.expectedSizeMb + 100) {
            return@withContext Result.failure(IllegalStateException("ডিভাইসে পর্যাপ্ত ফাঁকা স্টোরেজ নেই। অন্তত ${descriptor.expectedSizeMb + 100} MB খালি রাখুন।"))
        }

        updateProgress(descriptor.id, DownloadProgress(descriptor.id, isDownloading = true, progressPercent = 0))

        try {
            val request = Request.Builder().url(url).build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                throw IllegalStateException("সার্ভার রেসপন্স ব্যর্থ: ${response.code}")
            }

            val body = response.body ?: throw IllegalStateException("খালি রেসপন্স")
            val totalLength = if (body.contentLength() > 0) body.contentLength() else descriptor.expectedSizeMb * 1024 * 1024

            val input: InputStream = body.byteStream()
            val output = FileOutputStream(tempFile)
            val buffer = ByteArray(32 * 1024)
            var bytesCopied: Long = 0
            var read: Int
            var lastUpdateTime = System.currentTimeMillis()
            var bytesSinceLastUpdate = 0L

            while (input.read(buffer).also { read = it } != -1) {
                output.write(buffer, 0, read)
                bytesCopied += read
                bytesSinceLastUpdate += read

                val now = System.currentTimeMillis()
                if (now - lastUpdateTime > 400) {
                    val percent = ((bytesCopied * 100) / totalLength).toInt().coerceIn(0, 99)
                    val speed = (bytesSinceLastUpdate * 1000) / ((now - lastUpdateTime) * 1024)
                    updateProgress(
                        descriptor.id,
                        DownloadProgress(
                            modelId = descriptor.id,
                            isDownloading = true,
                            progressPercent = percent,
                            downloadedBytes = bytesCopied,
                            totalBytes = totalLength,
                            speedKbps = speed
                        )
                    )
                    lastUpdateTime = now
                    bytesSinceLastUpdate = 0
                }
            }

            output.flush()
            output.close()
            input.close()

            if (tempFile.exists()) {
                if (targetFile.exists()) targetFile.delete()
                tempFile.renameTo(targetFile)
            }

            updateProgress(descriptor.id, DownloadProgress(descriptor.id, isDownloading = false, progressPercent = 100))
            Result.success(Unit)
        } catch (e: Exception) {
            tempFile.delete()
            updateProgress(descriptor.id, DownloadProgress(descriptor.id, isDownloading = false, errorMessage = e.localizedMessage))
            Result.failure(e)
        }
    }

    suspend fun deleteModel(descriptor: ModelDescriptor): Boolean = withContext(Dispatchers.IO) {
        val file = getModelFile(descriptor)
        if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }

    private fun updateProgress(modelId: String, progress: DownloadProgress) {
        val current = _downloadProgress.value.toMutableMap()
        current[modelId] = progress
        _downloadProgress.value = current
    }
}
