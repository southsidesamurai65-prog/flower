# 识花 · Flower ID（Android）

拍照 / 选图 → 调用 OpenCode Go 的 **DeepSeek V4.1 Flash**（多模态）→ 返回按置信度排序的**最多 3 个**花卉候选，附科属、别名、判断依据与易混淆种提醒。

## 功能
- CameraX 拍照 + 相册导入（最多 3 张，可拍花 / 叶 / 果不同部位）
- 结果：中文名、拉丁学名、置信度、科、属、别名、判断依据、易混淆种、推理
- 本地历史记录（Room，含缩略图），可查看详情 / 删除
- 设置：API Key（加密存储）、模型、图片压缩参数
- 严格 JSON 结构化输出；主模型不接受图片时自动回退 `deepseek-v4-flash-vision-exp`

## 接口
- Endpoint：`https://opencode.ai/zen/go/v1/chat/completions`
- 模型：`deepseek-v4.1-flash`
- 鉴权：`Authorization: Bearer <OpenCode Go API Key>`
- 兼容 OpenAI Chat Completions；图片通过 `content` 数组的 `image_url` + base64 data URL 传入
- 请求头附带 `x-opencode-session`（稳定会话 ID）与自定义 `User-Agent`

> 注意：OpenCode Go 主要面向编码代理流量，官方会监控异常使用。请遵守其条款，仅用于个人学习。

## 构建 / 运行
本项目未包含 Gradle Wrapper 的二进制 jar。两种方式：

1. **Android Studio（推荐）**
   - `File → Open` 选择本目录 `FlowerID`
   - 首次会自动下载 Gradle 依赖并同步（需要 Android SDK 34/35、JDK 17）
   - 直接运行到真机/模拟器

2. **命令行**（需本机已装 Gradle 8.9+ 与 Android SDK，`local.properties` 里配置 `sdk.dir`）
   ```bash
   gradle wrapper --gradle-version 8.11.1   # 生成 wrapper（可选）
   ./gradlew assembleDebug
   ```
   调试包输出：`app/build/outputs/apk/debug/app-debug.apk`

首次启动后：进入「设置」填写你的 OpenCode Go API Key → 保存 → 回到「拍照」即可。

## 模块结构
```
app/src/main/java/com/example/flowerid/
├─ MainActivity.kt / FlowerIdApp.kt
├─ di/AppContainer.kt                 # 手动依赖容器
├─ data/api/VisionApi.kt              # OpenCode Go 调用 + 响应解析
├─ data/repo/IdentifyRepository.kt    # 压缩/调用/解析/入库/模型回退
├─ data/repo/PayloadParser.kt         # JSON 容错解析
├─ data/local/History.kt              # Room 实体 / DAO / 数据库
├─ data/model/Models.kt               # Candidate / IdentifyPayload ...
├─ prompt/Prompts.kt                  # 系统提示词 + JSON 约束
├─ security/ApiKeyStore.kt            # EncryptedSharedPreferences
├─ util/ImageUtils.kt                 # 缩放/方向校正/JPEG/Base64
├─ ui/MainScaffold.kt                 # 底部导航 + NavHost
├─ ui/IdentifyViewModel.kt
├─ ui/camera/CameraScreen.kt          # CameraX + 相册 + 多图
├─ ui/result/CandidateCard.kt         # 结果卡片
├─ ui/history/{HistoryScreen,HistoryViewModel}.kt
└─ ui/settings/{SettingsScreen,SettingsViewModel}.kt
```

## 主要依赖
Jetpack Compose (BOM 2024.12.01) · CameraX 1.4.1 · OkHttp 4.12 · kotlinx.serialization 1.7 ·
Room 2.6 · Coil 2.7 · security-crypto 1.1 · Kotlin 2.0.21 / AGP 8.7.3

## 安全
- API Key 仅保存在设备本地 `EncryptedSharedPreferences`，并已从备份规则中排除。
- 不在代码中硬编码任何密钥。
