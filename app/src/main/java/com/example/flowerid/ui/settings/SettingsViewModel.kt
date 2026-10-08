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
    val quality: Int = 85,
    val maxEdge: Int = 1280,
    val saved: Boolean = false,
)

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val store = (app as FlowerIdApp).container.apiKeyStore

    private val _state = MutableStateFlow(
        SettingsUiState(
            apiKey = store.getApiKey().orEmpty(),
            model = store.getModel(),
            quality = store.getQuality(),
            maxEdge = store.getMaxEdge(),
        ),
    )
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    fun setApiKey(value: String) = update { it.copy(apiKey = value, saved = false) }

    fun setModel(value: String) = update { it.copy(model = value, saved = false) }

    fun setQuality(value: Int) = update { it.copy(quality = value, saved = false) }

    fun setMaxEdge(value: Int) = update { it.copy(maxEdge = value, saved = false) }

    fun save() {
        val s = _state.value
        store.setApiKey(s.apiKey)
        store.setModel(s.model)
        store.setQuality(s.quality)
        store.setMaxEdge(s.maxEdge)
        _state.value = s.copy(saved = true)
    }

    private inline fun update(block: (SettingsUiState) -> SettingsUiState) {
        _state.value = block(_state.value)
    }
}
