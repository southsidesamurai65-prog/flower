package com.example.flowerid.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.flowerid.FlowerIdApp
import com.example.flowerid.data.local.HistoryItem
import com.example.flowerid.data.repo.HistoryFilter
import com.example.flowerid.data.repo.PayloadParser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HistoryViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as FlowerIdApp).container.historyRepository

    private val _filters = MutableStateFlow(HistoryFilter())
    val filters: StateFlow<HistoryFilter> = _filters.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val items: StateFlow<List<HistoryItem>> = _filters
        .flatMapLatest { repo.observeFiltered(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<String>> = repo.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setCategory(value: String?) = _filters.update { it.copy(category = value) }

    fun setLeafForm(value: String?) = _filters.update { it.copy(leafForm = value) }

    fun setLeafShape(value: String?) = _filters.update { it.copy(leafShape = value) }

    fun setLeafArrangement(value: String?) = _filters.update { it.copy(leafArrangement = value) }

    fun setLeafMargin(value: String?) = _filters.update { it.copy(leafMargin = value) }

    fun setFlowerShape(value: String?) = _filters.update { it.copy(flowerShape = value) }

    fun setInflorescence(value: String?) = _filters.update { it.copy(inflorescence = value) }

    fun setOvaryPosition(value: String?) = _filters.update { it.copy(ovaryPosition = value) }

    fun setFruitType(value: String?) = _filters.update { it.copy(fruitType = value) }

    fun clearFilters() {
        _filters.value = HistoryFilter()
    }

    fun delete(id: Long) {
        viewModelScope.launch { repo.delete(id) }
    }

    fun detail(item: HistoryItem) = PayloadParser.fromJson(item.resultJson)
}
