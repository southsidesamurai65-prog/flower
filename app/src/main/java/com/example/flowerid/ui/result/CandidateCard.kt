package com.example.flowerid.ui.result

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.flowerid.data.model.Candidate
import com.example.flowerid.data.model.IdentifyPayload
import com.example.flowerid.util.PlantLinks

@Composable
fun CandidateList(
    payload: IdentifyPayload,
    modifier: Modifier = Modifier,
    onArchive: ((Candidate) -> Unit)? = null,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        payload.candidates.forEachIndexed { index, candidate ->
            CandidateCard(
                rank = index + 1,
                candidate = candidate,
                onArchive = onArchive?.let { archive -> { archive(candidate) } },
            )
        }
        if (payload.note.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Text(
                    text = "说明：${payload.note}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
    }
}

@Composable
fun CandidateCard(
    rank: Int,
    candidate: Candidate,
    modifier: Modifier = Modifier,
    onArchive: (() -> Unit)? = null,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "#$rank",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = candidate.name.ifBlank { "未确定" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (candidate.scientificName.isNotBlank()) {
                        Text(
                            text = candidate.scientificName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                        )
                    }
                }
                Text(
                    text = "${(candidate.confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }

            LinearProgressIndicator(
                progress = { candidate.confidence.coerceIn(0.0, 1.0).toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )

            if (candidate.family.isNotBlank() || candidate.genus.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (candidate.family.isNotBlank()) ChipLike("科", candidate.family)
                    if (candidate.genus.isNotBlank()) ChipLike("属", candidate.genus)
                }
            }

            TagChips(candidate)

            if (candidate.aliases.isNotEmpty()) {
                LabeledLine("别名", candidate.aliases.joinToString("、"))
            }

            if (candidate.features.isNotEmpty()) {
                LabeledBlock("判断依据", candidate.features.map { "• $it" })
            }

            if (candidate.confusableWith.isNotEmpty()) {
                LabeledBlock(
                    title = "易混淆",
                    lines = candidate.confusableWith.map {
                        "• ${it.name}：${it.difference}"
                    },
                )
            }

            if (candidate.reasoning.isNotBlank()) {
                LabeledLine("推理", candidate.reasoning)
            }

            CandidateActions(candidate = candidate, onArchive = onArchive)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagChips(candidate: Candidate) {
    val tags = candidate.tags
    val pairs = buildList {
        tags.leafForm.takeIf { it.isNotBlank() }?.let { add("叶性" to it) }
        tags.leafShape.takeIf { it.isNotBlank() }?.let { add("叶形" to it) }
        tags.leafArrangement.takeIf { it.isNotBlank() }?.let { add("叶序" to it) }
        tags.leafMargin.takeIf { it.isNotBlank() }?.let { add("叶缘" to it) }
        tags.flowerShape.takeIf { it.isNotBlank() }?.let { add("花冠" to it) }
        tags.inflorescence.takeIf { it.isNotBlank() }?.let { add("花序" to it) }
        tags.ovaryPosition.takeIf { it.isNotBlank() }?.let { add("子房" to it) }
        tags.fruitType.takeIf { it.isNotBlank() }?.let { add("果实" to it) }
    }
    if (pairs.isEmpty()) return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        pairs.forEach { (label, value) -> ChipLike(label, value) }
    }
}

@Composable
private fun CandidateActions(candidate: Candidate, onArchive: (() -> Unit)?) {
    var menuOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val sites = remember(candidate.name, candidate.scientificName) {
        PlantLinks.sites(candidate.name, candidate.scientificName)
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            OutlinedButton(onClick = { menuOpen = true }, enabled = sites.isNotEmpty()) {
                Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("查证")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                sites.forEach { site ->
                    DropdownMenuItem(
                        text = { Text(site.label) },
                        onClick = {
                            menuOpen = false
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(site.url)))
                            }
                        },
                    )
                }
            }
        }
        if (onArchive != null) {
            Button(onClick = onArchive) { Text("归档") }
        }
    }
}

@Composable
private fun ChipLike(label: String, value: String) {
    AssistChip(
        onClick = {},
        label = { Text("$label $value", style = MaterialTheme.typography.labelSmall) },
    )
}

@Composable
private fun LabeledLine(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun LabeledBlock(title: String, lines: List<String>) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(2.dp))
        lines.forEach { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
    }
}
