package com.example.flowerid.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.flowerid.FlowerIdApp
import com.example.flowerid.data.local.HistoryItem
import com.example.flowerid.data.repo.PayloadParser
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as FlowerIdApp).container.historyRepository

    val items: StateFlow<List<HistoryItem>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(id: Long) {
        viewModelScope.launch { repo.delete(id) }
    }

    fun detail(item: HistoryItem) = PayloadParser.fromJson(item.resultJson)
}
