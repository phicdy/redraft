# Redraft

A personal English writing assistant for macOS and Android.

- **Translate**: Japanese input is translated into English, with grammar points and expressions worth learning.
- **Proofread**: English input is corrected, with explanations and more natural alternatives.
- **Report**: Weekly reports summarize recurring mistakes and progress from history.

## Tech stack

Kotlin Multiplatform + Compose Multiplatform (Android and desktop JVM), Ktor, kotlinx.serialization, Koin.
LLM providers are accessed through a provider-independent adapter layer (`shared/llm`).

## Modules

| Module | Role |
| --- | --- |
| `androidApp` | Android entry point |
| `desktopApp` | macOS (desktop JVM) entry point and dmg packaging |
| `shared/model` | Domain types: results, error categories, input mode detection |
| `shared/llm` | `LlmClient` interface, provider adapters, prompts |
| `shared/data` | History storage, API key storage, settings |
| `shared/ui` | Compose screens and ViewModels |

Dependencies point one way: `ui → data / llm → model`.

## Build

```sh
./gradlew desktopTest                # unit tests
./gradlew :androidApp:assembleDebug  # Android
./gradlew :desktopApp:run            # run the desktop app
./gradlew :desktopApp:packageDmg     # macOS dmg (run on macOS)
```

Requires JDK 17 or later.

## Compose previews on pull requests

Previews listed in `shared/ui/src/desktopTest/.../screenshot/PreviewCatalog.kt` are rendered on the desktop (JVM)
target in light and dark theme by `PreviewScreenshotTest`, pushed to the `screenshots` branch, and posted as a
comment on the pull request. When you add a `@Preview`, add it to `PreviewCatalog` as well.

```sh
./gradlew :shared:ui:desktopTest --tests '*PreviewScreenshotTest'  # writes shared/ui/build/screenshots/*.png
```
