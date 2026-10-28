package com.example.flowerid.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flowerid.security.ApiKeyStore

private val PRESET_MODELS = listOf(
    ApiKeyStore.DEFAULT_MODEL,
    ApiKeyStore.FALLBACK_MODEL,
    "deepseek-v4-pro",
)

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showKey by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("OpenCode Go 配置", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

        OutlinedTextField(
            value = state.apiKey,
            onValueChange = viewModel::setApiKey,
            label = { Text("API Key") },
            singleLine = true,
            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showKey = !showKey }) {
                    Icon(
                        imageVector = if (showKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (showKey) "隐藏" else "显示",
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "在 opencode.ai/auth 订阅 OpenCode Go 后获取 API Key，仅保存在本机加密存储中。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text("接口地址", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = state.endpoint,
            onValueChange = viewModel::setEndpoint,
            label = { Text("Chat Completions URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "地址会原样使用，不做任何拼接或补全。以 /messages 结尾时按 Anthropic 协议发送，其余按 OpenAI Chat Completions；模型 ID 需与该接口匹配。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(
            onClick = { viewModel.setEndpoint(ApiKeyStore.DEFAULT_ENDPOINT) },
        ) { Text("恢复默认") }

        Text("模型", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PRESET_MODELS.forEach { model ->
                FilterChip(
                    selected = state.model == model,
                    onClick = { viewModel.setModel(model) },
                    label = {
                        Text(
                            text = if (model == ApiKeyStore.DEFAULT_MODEL) "V4.1 Flash" else model,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                )
            }
        }
        OutlinedTextField(
            value = state.model,
            onValueChange = viewModel::setModel,
            label = { Text("模型 ID（可自定义）") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text("图片压缩", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text("长边上限：${state.maxEdge} px", style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = state.maxEdge.toFloat(),
            onValueChange = { viewModel.setMaxEdge(it.toInt()) },
            valueRange = 480f..2048f,
            steps = 9,
        )
        Text("JPEG 质量：${state.quality}", style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = state.quality.toFloat(),
            onValueChange = { viewModel.setQuality(it.toInt()) },
            valueRange = 40f..100f,
            steps = 11,
        )

        Text("最大输出 tokens：${state.maxTokens}", style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = state.maxTokens.toFloat(),
            onValueChange = { viewModel.setMaxTokens(it.toInt()) },
            valueRange = 1024f..32768f,
            steps = 30,
        )
        Text(
            text = "仅 Anthropic 接口使用；思考内容也计入该上限，若只返回 thinking 没有正文就调高它。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Save, contentDescription = null)
            Spacer(Modifier.height(0.dp))
            Text("  保存")
        }
        if (state.saved) {
            Text("已保存", color = MaterialTheme.colorScheme.primary)
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("当前请求地址（原样使用）", style = MaterialTheme.typography.labelLarge)
                Text(
                    text = state.endpoint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
