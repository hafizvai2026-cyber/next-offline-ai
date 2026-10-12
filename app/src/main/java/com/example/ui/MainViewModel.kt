package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.EmbeddedBengaliLlmEngine
import com.example.ai.InferenceConfig
import com.example.ai.LlmEngine
import com.example.ai.MediaPipeLlmEngine
import com.example.ai.ModelDescriptor
import com.example.ai.ModelManager
import com.example.data.local.AppDatabase
import com.example.data.local.ChatRepository
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageEntity
import com.example.data.local.PreferencesManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UiState(
    val currentConversationId: String? = null,
    val currentConversationTitle: String = "কথোপকথন",
    val messages: List<MessageEntity> = emptyList(),
    val isGenerating: Boolean = false,
    val streamingChunk: String = "",
    val activeModelName: String = "Next On-Device AI",
    val activeModelId: String = "embedded_bengali",
    val searchQuery: String = "",
    val errorNotice: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val chatRepository = ChatRepository(db.conversationDao(), db.messageDao())
    val preferencesManager = PreferencesManager(application)
    val modelManager = ModelManager(application)

    private val embeddedEngine = EmbeddedBengaliLlmEngine()
    private val mediaPipeEngine = MediaPipeLlmEngine()
    private var currentEngine: LlmEngine = embeddedEngine

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val conversations: StateFlow<List<ConversationEntity>> = chatRepository.allConversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var generationJob: Job? = null
    private var messageCollectionJob: Job? = null

    init {
        viewModelScope.launch {
            // Load preferred model
            val modelId = preferencesManager.selectedModelFlow.first()
            val descriptor = modelManager.availableModels.find { it.id == modelId }
                ?: modelManager.availableModels.first()
            loadEngine(descriptor)
        }
    }

    private suspend fun loadEngine(descriptor: ModelDescriptor) {
        val config = getCurrentConfig()
        if (descriptor.isBuiltIn) {
            currentEngine = embeddedEngine
            embeddedEngine.initialize(getApplication(), null, config)
            _uiState.value = _uiState.value.copy(
                activeModelName = descriptor.name,
                activeModelId = descriptor.id
            )
        } else {
            val file = modelManager.getModelFile(descriptor)
            if (file.exists()) {
                currentEngine = mediaPipeEngine
                val res = mediaPipeEngine.initialize(getApplication(), file, config)
                if (res.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        activeModelName = descriptor.name,
                        activeModelId = descriptor.id
                    )
                } else {
                    currentEngine = embeddedEngine
                    _uiState.value = _uiState.value.copy(
                        activeModelName = "Next On-Device AI (ফলব্যাক)",
                        activeModelId = "embedded_bengali",
                        errorNotice = "মডেল লোড ব্যর্থ হয়েছে, অফলাইন ফলব্যাক ইঞ্জিন সক্রিয়।"
                    )
                }
            } else {
                currentEngine = embeddedEngine
                _uiState.value = _uiState.value.copy(
                    activeModelName = "Next On-Device AI",
                    activeModelId = "embedded_bengali"
                )
            }
        }
    }

    fun selectConversation(conversationId: String) {
        _uiState.value = _uiState.value.copy(
            currentConversationId = conversationId,
            streamingChunk = "",
            isGenerating = false
        )
        messageCollectionJob?.cancel()
        messageCollectionJob = viewModelScope.launch {
            val conv = chatRepository.getConversation(conversationId)
            if (conv != null) {
                _uiState.value = _uiState.value.copy(currentConversationTitle = conv.title)
            }
            chatRepository.getMessagesForConversation(conversationId).collectLatest { msgs ->
                _uiState.value = _uiState.value.copy(messages = msgs)
            }
        }
    }

    fun startNewConversation(initialInstruction: String? = null, onCreated: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            val newConv = chatRepository.createNewConversation(
                title = "নতুন কথোপকথন",
                initialInstruction = initialInstruction
            )
            selectConversation(newConv.id)
            onCreated?.invoke(newConv.id)
        }
    }

    fun sendMessage(userText: String) {
        val clean = userText.trim()
        if (clean.isBlank()) return

        var convId = _uiState.value.currentConversationId
        viewModelScope.launch {
            if (convId == null) {
                val newConv = chatRepository.createNewConversation()
                convId = newConv.id
                selectConversation(convId)
            }

            chatRepository.saveUserMessage(convId!!, clean)

            // Gather recent history for on-device context memory
            val recentMessages = chatRepository.getRecentContext(convId!!, limit = 8)
            val historyPairs = recentMessages.map { it.role to it.content }

            _uiState.value = _uiState.value.copy(
                isGenerating = true,
                streamingChunk = ""
            )

            val config = getCurrentConfig()
            val streamedBuilder = StringBuilder()

            generationJob = launch {
                try {
                    currentEngine.generateStream(clean, historyPairs, config).collect { chunk ->
                        streamedBuilder.append(chunk)
                        _uiState.value = _uiState.value.copy(streamingChunk = streamedBuilder.toString())
                    }

                    // Save completed assistant response
                    val finalAnswer = streamedBuilder.toString()
                    chatRepository.saveAssistantMessage(
                        conversationId = convId!!,
                        content = finalAnswer,
                        modelName = _uiState.value.activeModelName
                    )
                } catch (e: Exception) {
                    chatRepository.saveAssistantMessage(
                        conversationId = convId!!,
                        content = "ইনফারেন্স প্রক্রিয়ায় সমস্যা হয়েছে: ${e.localizedMessage ?: "অজ্ঞাত ত্রুটি"}",
                        modelName = _uiState.value.activeModelName,
                        isError = true
                    )
                } finally {
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        streamingChunk = ""
                    )
                }
            }
        }
    }

    fun stopGeneration() {
        currentEngine.stop()
        generationJob?.cancel()
        val partial = _uiState.value.streamingChunk
        val convId = _uiState.value.currentConversationId
        if (convId != null && partial.isNotBlank()) {
            viewModelScope.launch {
                chatRepository.saveAssistantMessage(
                    conversationId = convId,
                    content = "$partial\n\n*[থামানো হয়েছে]*",
                    modelName = _uiState.value.activeModelName
                )
            }
        }
        _uiState.value = _uiState.value.copy(
            isGenerating = false,
            streamingChunk = ""
        )
    }

    fun regenerateLastResponse() {
        val msgs = _uiState.value.messages
        val lastUserMsg = msgs.lastOrNull { it.role == "user" } ?: return
        sendMessage(lastUserMsg.content)
    }

    fun renameConversation(id: String, newTitle: String) {
        viewModelScope.launch {
            chatRepository.renameConversation(id, newTitle)
            if (_uiState.value.currentConversationId == id) {
                _uiState.value = _uiState.value.copy(currentConversationTitle = newTitle)
            }
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            chatRepository.deleteConversation(id)
            if (_uiState.value.currentConversationId == id) {
                _uiState.value = _uiState.value.copy(
                    currentConversationId = null,
                    messages = emptyList(),
                    currentConversationTitle = "কথোপকথন"
                )
            }
        }
    }

    fun clearAllConversations() {
        viewModelScope.launch {
            chatRepository.clearAllHistory()
            _uiState.value = _uiState.value.copy(
                currentConversationId = null,
                messages = emptyList(),
                currentConversationTitle = "কথোপকথন"
            )
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun selectModel(descriptor: ModelDescriptor) {
        viewModelScope.launch {
            preferencesManager.setSelectedModel(descriptor.id)
            loadEngine(descriptor)
        }
    }

    fun downloadModel(descriptor: ModelDescriptor) {
        viewModelScope.launch {
            modelManager.downloadModel(descriptor)
        }
    }

    fun deleteModel(descriptor: ModelDescriptor) {
        viewModelScope.launch {
            modelManager.deleteModel(descriptor)
            if (_uiState.value.activeModelId == descriptor.id) {
                selectModel(modelManager.availableModels.first())
            }
        }
    }

    private suspend fun getCurrentConfig(): InferenceConfig {
        return InferenceConfig(
            temperature = preferencesManager.temperatureFlow.first(),
            maxTokens = preferencesManager.maxTokensFlow.first(),
            systemPrompt = preferencesManager.customInstructionFlow.first(),
            performancePreset = preferencesManager.presetFlow.first()
        )
    }
}
