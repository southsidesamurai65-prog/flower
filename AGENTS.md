# AGENTS.md

Android flower-identification app "识花 · Flower ID" (`com.example.flowerid`). Single Gradle
module `:app`; Kotlin + Jetpack Compose + CameraX + Room + OkHttp. Photos are sent to the
OpenCode Go OpenAI-compatible chat-completions endpoint for vision identification.

## Build & verify
- **There is no `gradlew`/wrapper jar committed** — only `gradle/wrapper/gradle-wrapper.properties`
  (requests Gradle 8.11.1). `./gradlew` fails until someone runs `gradle wrapper` once.
- Use a system Gradle 8.9+: `gradle --no-daemon assembleDebug`
- Needs Android SDK (platform 35) + JDK 17+. SDK location comes from gitignored `local.properties`
  (`sdk.dir=...`) or `ANDROID_HOME`.
- **Windows/non-ASCII path quirk:** the checkout path (`大四\花卉`) trips AGP's path check. This is
  already worked around by `android.overridePathCheck=true` in `gradle.properties`; removing it
  makes every build abort immediately.
- Verification command: `gradle assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk` (~20 MB).
  Faster check: `gradle :app:compileDebugKotlin`.
- **No test sources and no test dependencies exist.** `gradle test` / `connectedAndroidTest` run
  nothing — never claim tests pass; build success is the only automated signal.

## Gotchas
- `app/build/` is untracked and **not** ignored (`.gitignore` only ignores root `/build`). Never
  commit build output.
- Release build has no signing config; only debug builds are runnable.
- Room DB `flowerid.db` uses `fallbackToDestructiveMigration()`. Changing `HistoryItem` fields
  without bumping `@Database(version = ...)` silently wipes history.
- API key + settings live in `EncryptedSharedPreferences` (`security/ApiKeyStore.kt`), never in
  `BuildConfig` or source. Do not hardcode any secret.
- Endpoint is hardcoded: `VisionApi.GO_ENDPOINT` = `https://opencode.ai/zen/go/v1/chat/completions`.
- Default model `deepseek-v4.1-flash`; `IdentifyRepository` retries `deepseek-v4-flash-vision-exp`
  only on HTTP 400/404/422.
- Model reply is strict JSON mapped in `data/model/Models.kt` via kotlinx.serialization
  `@SerialName` (`scientific_name`, `confusable_with`); `PayloadParser` strips fences/prose first.
  Keep `prompt/Prompts.kt` and `Models.kt` in sync (both use Chinese field prose).

## Architecture
- Manual DI (no Hilt/Koin): `FlowerIdApp` builds `di/AppContainer.kt`; ViewModels read it via
  `(app as FlowerIdApp).container`.
- `MainActivity` → `ui/MainScaffold.kt` (Compose Navigation, tabs: camera / history / settings).
- Flow: `ui/camera/CameraScreen` → `IdentifyViewModel.identify()` → `IdentifyRepository`
  (compress with `util/ImageUtils` → call `data/api/VisionApi` → parse → persist) → Room.
- Identify accepts max 3 images. History stores the payload JSON plus a 320px thumbnail of only
  the first image (`filesDir/thumbs`).
