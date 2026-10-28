package com.example.flowerid.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.flowerid.FlowerIdApp
import com.example.flowerid.security.ApiKeyStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsUiState(
    val apiKey: String = "",
    val model: String = ApiKeyStore.DEFAULT_MODEL,
    val endpoint: String = ApiKeyStore.DEFAULT_ENDPOINT,
    val quality: Int = 85,
    val maxEdge: Int = 1280,
    val maxTokens: Int = ApiKeyStore.DEFAULT_MAX_TOKENS,
    val saved: Boolean = false,
)

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val store = (app as FlowerIdApp).container.apiKeyStore

    private val _state = MutableStateFlow(
        SettingsUiState(
            apiKey = store.getApiKey().orEmpty(),
            model = store.getModel(),
            endpoint = store.getEndpoint(),
            quality = store.getQuality(),
            maxEdge = store.getMaxEdge(),
            maxTokens = store.getMaxTokens(),
        ),
    )
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    fun setApiKey(value: String) = update { it.copy(apiKey = value, saved = false) }

    fun setModel(value: String) = update { it.copy(model = value, saved = false) }

    fun setEndpoint(value: String) = update { it.copy(endpoint = value, saved = false) }

    fun setQuality(value: Int) = update { it.copy(quality = value, saved = false) }

    fun setMaxEdge(value: Int) = update { it.copy(maxEdge = value, saved = false) }

    fun setMaxTokens(value: Int) = update { it.copy(maxTokens = value, saved = false) }

    fun save() {
        val s = _state.value
        store.setApiKey(s.apiKey)
        store.setModel(s.model)
        store.setEndpoint(s.endpoint)
        store.setQuality(s.quality)
        store.setMaxEdge(s.maxEdge)
        store.setMaxTokens(s.maxTokens)
        _state.value = s.copy(saved = true)
    }

    private inline fun update(block: (SettingsUiState) -> SettingsUiState) {
        _state.value = block(_state.value)
    }
}
