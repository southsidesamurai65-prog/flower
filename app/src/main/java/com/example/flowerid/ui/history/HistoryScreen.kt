package com.example.flowerid.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.flowerid.data.local.HistoryItem
import com.example.flowerid.data.model.IdentifyPayload
import com.example.flowerid.data.model.PlantTags
import com.example.flowerid.data.repo.HistoryFilter
import com.example.flowerid.ui.result.CandidateList
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = viewModel()) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    var detail by remember { mutableStateOf<IdentifyPayload?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        FilterPanel(filters = filters, categories = categories, viewModel = viewModel)

        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (filters.isEmpty) "还没有归档记录" else "没有符合筛选条件的记录",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items, key = { it.id }) { item ->
                    HistoryRow(
                        item = item,
                        onClick = { detail = viewModel.detail(item) },
                        onDelete = { viewModel.delete(item.id) },
                    )
                }
            }
        }
    }

    detail?.let { payload ->
        AlertDialog(
            onDismissRequest = { detail = null },
            confirmButton = {
                TextButton(onClick = { detail = null }) { Text("关闭") }
            },
            title = { Text("识别结果") },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 460.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    CandidateList(payload = payload)
                }
            },
        )
    }
}

@Composable
private fun FilterPanel(
    filters: HistoryFilter,
    categories: List<String>,
    viewModel: HistoryViewModel,
) {
    var expanded by remember { mutableStateOf(false) }
    val active = activeCount(filters)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.FilterList, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (active == 0) "筛选：全部" else "筛选：已选 $active 项",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                if (active > 0) {
                    TextButton(onClick = viewModel::clearFilters) { Text("清空") }
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "收起" else "展开",
                )
            }

            if (expanded) {
                Spacer(Modifier.size(8.dp))
                Column(
                    modifier = Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("分类", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    FilterDropdown("科", categories, filters.category, viewModel::setCategory)

                    Text("叶", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    FilterDropdown("叶性", PlantTags.LEAF_FORMS, filters.leafForm, viewModel::setLeafForm)
                    FilterDropdown("叶形", PlantTags.LEAF_SHAPES, filters.leafShape, viewModel::setLeafShape)
                    FilterDropdown("叶序", PlantTags.LEAF_ARRANGEMENTS, filters.leafArrangement, viewModel::setLeafArrangement)
                    FilterDropdown("叶缘", PlantTags.LEAF_MARGINS, filters.leafMargin, viewModel::setLeafMargin)

                    Text("花", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    FilterDropdown("花冠形状", PlantTags.FLOWER_SHAPES, filters.flowerShape, viewModel::setFlowerShape)
                    FilterDropdown("花序类型", PlantTags.INFLORESCENCES, filters.inflorescence, viewModel::setInflorescence)
                    FilterDropdown("子房位置", PlantTags.OVARY_POSITIONS, filters.ovaryPosition, viewModel::setOvaryPosition)

                    Text("果", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    FilterDropdown("果实类型", PlantTags.FRUIT_TYPES, filters.fruitType, viewModel::setFruitType)
                }
            }
        }
    }
}

@Composable
private fun FilterDropdown(
    label: String,
    options: List<String>,
    value: String?,
    onValueChange: (String?) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
                Text(text = value ?: "全部", maxLines = 1)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                DropdownMenuItem(
                    text = { Text("全部") },
                    onClick = {
                        open = false
                        onValueChange(null)
                    },
                )
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            open = false
                            onValueChange(option)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    item: HistoryItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = File(item.thumbPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = formatTime(item.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val tagLine = listOfNotNull(
                    item.category.takeIf { it.isNotBlank() },
                    item.leafForm.takeIf { it.isNotBlank() },
                    item.leafShape.takeIf { it.isNotBlank() },
                    item.flowerShape.takeIf { it.isNotBlank() },
                    item.fruitType.takeIf { it.isNotBlank() },
                ).joinToString(" · ")
                if (tagLine.isNotBlank()) {
                    Text(
                        text = tagLine,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = item.modelUsed,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "删除")
            }
        }
    }
}

private fun activeCount(filters: HistoryFilter): Int = listOf(
    filters.category, filters.leafForm, filters.leafShape, filters.leafArrangement, filters.leafMargin,
    filters.flowerShape, filters.inflorescence, filters.ovaryPosition, filters.fruitType,
).count { !it.isNullOrBlank() }

private fun formatTime(millis: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(millis))
