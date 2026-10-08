package com.example.flowerid.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.flowerid.FlowerIdApp
import com.example.flowerid.data.api.ApiException
import com.example.flowerid.data.model.IdentifyResult
import com.example.flowerid.data.model.MissingApiKeyException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

data class IdentifyUiState(
    val selected: List<Uri> = emptyList(),
    val loading: Boolean = false,
    val result: IdentifyResult? = null,
    val error: String? = null,
    val hasApiKey: Boolean = false,
)

class IdentifyViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as FlowerIdApp).container

    private val _state = MutableStateFlow(IdentifyUiState(hasApiKey = container.apiKeyStore.getApiKey() != null))
    val state: StateFlow<IdentifyUiState> = _state.asStateFlow()

    fun refreshApiKeyStatus() {
        _state.update { it.copy(hasApiKey = container.apiKeyStore.getApiKey() != null) }
    }

    fun addImage(uri: Uri) {
        _state.update { current ->
            if (current.selected.contains(uri)) current
            else current.copy(selected = (current.selected + uri).take(3), error = null)
        }
    }

    fun addImages(uris: List<Uri>) {
        _state.update { current ->
            val merged = (current.selected + uris).distinct().take(3)
            current.copy(selected = merged, error = null)
        }
    }

    fun removeImage(uri: Uri) {
        _state.update { it.copy(selected = it.selected.filterNot { u -> u == uri }) }
    }

    fun clearResult() {
        _state.update { it.copy(result = null, error = null) }
    }

    fun identify() {
        val uris = _state.value.selected
        if (uris.isEmpty()) {
            _state.update { it.copy(error = "请先拍摄或选择至少一张照片") }
            return
        }
        if (container.apiKeyStore.getApiKey() == null) {
            _state.update { it.copy(error = "请先在「设置」中填写 OpenCode Go API Key", hasApiKey = false) }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val result = container.identifyRepository.identify(uris)
                _state.update { it.copy(loading = false, result = result, error = null) }
            } catch (e: MissingApiKeyException) {
                _state.update { it.copy(loading = false, error = e.message, hasApiKey = false) }
            } catch (e: ApiException) {
                _state.update { it.copy(loading = false, error = describeApiError(e)) }
            } catch (e: IOException) {
                _state.update { it.copy(loading = false, error = "网络错误：${e.message ?: "请检查网络连接"}") }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = "识别失败：${e.message ?: "未知错误"}") }
            }
        }
    }

    private fun describeApiError(e: ApiException): String = when (e.code) {
        401, 403 -> "API Key 无效或权限不足，请到「设置」检查"
        402 -> "账户余额/额度不足"
        429 -> "请求过于频繁，请稍后再试"
        in 500..599 -> "服务端错误（${e.code}），请稍后再试"
        else -> "请求失败（${e.code}）"
    }
}
