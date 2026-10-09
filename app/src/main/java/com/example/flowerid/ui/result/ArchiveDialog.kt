package com.example.flowerid.ui.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.example.flowerid.data.model.Candidate
import com.example.flowerid.data.model.PlantTags

/** Lets the user confirm/edit the model's proposed tags before saving the chosen candidate. */
@Composable
fun ArchiveDialog(
    candidate: Candidate,
    onDismiss: () -> Unit,
    onConfirm: (Candidate) -> Unit,
) {
    var family by remember { mutableStateOf(PlantTags.normalizeFamily(candidate.family)) }
    var leafForm by remember { mutableStateOf(PlantTags.normalize(candidate.tags.leafForm, PlantTags.LEAF_FORMS)) }
    var leafShape by remember { mutableStateOf(PlantTags.normalize(candidate.tags.leafShape, PlantTags.LEAF_SHAPES)) }
    var leafArrangement by remember { mutableStateOf(PlantTags.normalize(candidate.tags.leafArrangement, PlantTags.LEAF_ARRANGEMENTS)) }
    var leafMargin by remember { mutableStateOf(PlantTags.normalize(candidate.tags.leafMargin, PlantTags.LEAF_MARGINS)) }
    var flowerShape by remember { mutableStateOf(PlantTags.normalize(candidate.tags.flowerShape, PlantTags.FLOWER_SHAPES)) }
    var inflorescence by remember { mutableStateOf(PlantTags.normalize(candidate.tags.inflorescence, PlantTags.INFLORESCENCES)) }
    var ovaryPosition by remember { mutableStateOf(PlantTags.normalize(candidate.tags.ovaryPosition, PlantTags.OVARY_POSITIONS)) }
    var fruitType by remember { mutableStateOf(PlantTags.normalize(candidate.tags.fruitType, PlantTags.FRUIT_TYPES)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("归档 · ${candidate.name.ifBlank { "未识别" }}") },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        candidate.copy(
                            family = family,
                            tags = candidate.tags.copy(
                                leafForm = leafForm,
                                leafShape = leafShape,
                                leafArrangement = leafArrangement,
                                leafMargin = leafMargin,
                                flowerShape = flowerShape,
                                inflorescence = inflorescence,
                                ovaryPosition = ovaryPosition,
                                fruitType = fruitType,
                            ),
                        ),
                    )
                },
            ) { Text("归档") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (candidate.scientificName.isNotBlank()) {
                    Text(
                        text = candidate.scientificName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                OutlinedTextField(
                    value = family,
                    onValueChange = { family = it },
                    label = { Text("分类（科）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                SectionHeader("叶")
                TagDropdown("叶性", PlantTags.LEAF_FORMS, leafForm) { leafForm = it }
                TagDropdown("叶形", PlantTags.LEAF_SHAPES, leafShape) { leafShape = it }
                TagDropdown("叶序", PlantTags.LEAF_ARRANGEMENTS, leafArrangement) { leafArrangement = it }
                TagDropdown("叶缘", PlantTags.LEAF_MARGINS, leafMargin) { leafMargin = it }

                SectionHeader("花")
                TagDropdown("花冠形状", PlantTags.FLOWER_SHAPES, flowerShape) { flowerShape = it }
                TagDropdown("花序类型", PlantTags.INFLORESCENCES, inflorescence) { inflorescence = it }
                TagDropdown("子房位置", PlantTags.OVARY_POSITIONS, ovaryPosition) { ovaryPosition = it }

                SectionHeader("果")
                TagDropdown("果实类型", PlantTags.FRUIT_TYPES, fruitType) { fruitType = it }
            }
        },
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun TagDropdown(
    label: String,
    options: List<String>,
    value: String,
    onValueChange: (String) -> Unit,
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
                Text(text = value.ifBlank { "未设置" }, maxLines = 1)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                DropdownMenuItem(
                    text = { Text("未设置") },
                    onClick = {
                        open = false
                        onValueChange("")
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
