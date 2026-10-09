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
- Room DB `flowerid.db` uses `fallbackToDestructiveMigration()`. It is at `@Database(version = 2)`.
  Changing `HistoryItem` fields without bumping the version silently wipes history (and the
  fallback already wipes on any mismatch).
- Identify does **not** persist automatically: history is written only when the user archives a
  candidate (`IdentifyRepository.archive`), so don't re-add a save inside `identify()`.
- Tag vocabularies are fixed and centralized in `data/model/PlantTags.kt` (leaf form/shape/arrangement/
  margin, flower shape, inflorescence, ovary position, fruit type). `prompt/Prompts.kt` renders its
  option lists from `PlantTags`; add a new value/dimension in one place only. Family (科) is not
  enumerated — history filter options come from `SELECT DISTINCT category`. Model values are snapped
  onto the lists via `PlantTags.normalize` (unknown → `其他`); `PlantTags.normalizeFamily` strips the
  Latin suffix from `Candidate.family`.
- `Candidate.tags` is the nested `CandidateTags` object (`@SerialName` snake_case). `PayloadParser`
  tolerates missing tags (all fields default to ""), so old JSON still decodes.
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
  (compress with `util/ImageUtils` → call `data/api/VisionApi` → parse) → result is held in state.
  The user then verifies via `util/PlantLinks` (opens PPBC / iPlant / Baidu Baike), archives a
  candidate through `ArchiveDialog` (`IdentifyViewModel.archive` → Room), or rejects/re-identifies.
- Identify accepts max 3 images. `HistoryItem` stores the chosen candidate's normalized tags plus
  the full payload JSON and a 320px thumbnail of only the first image (`filesDir/thumbs`). History
  filters by the 9 tag columns (see `data/repo/HistoryRepository.kt` `HistoryFilter`).
