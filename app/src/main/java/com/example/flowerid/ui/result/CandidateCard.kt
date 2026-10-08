package com.example.flowerid.ui.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.flowerid.data.model.Candidate
import com.example.flowerid.data.model.IdentifyPayload

@Composable
fun CandidateList(payload: IdentifyPayload, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        payload.candidates.forEachIndexed { index, candidate ->
            CandidateCard(rank = index + 1, candidate = candidate)
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
fun CandidateCard(rank: Int, candidate: Candidate, modifier: Modifier = Modifier) {
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
