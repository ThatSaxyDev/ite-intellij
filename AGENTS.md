# AGENTS.md

## Project Overview

**Project:** `ite-intellij`

IntelliJ Platform plugin for iTE. It mirrors the VS Code extension by launching the iTE CLI in the IDE terminal, resolving a configured or managed runtime, and installing official releases from `ite.kiishi.space` when needed.

## Architecture

```
ite-intellij/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── src/main/
    ├── kotlin/com/kiishi/ite/intellij/
    │   ├── actions/       # Action handlers and user notifications
    │   ├── runtime/       # Runtime detection and managed install
    │   ├── settings/      # Persistent settings and settings UI
    │   ├── startup/       # Project startup checks
    │   └── terminal/      # IntelliJ Terminal integration
    └── resources/META-INF/plugin.xml
```

## Development Guidelines

- Kotlin source targets Java 17.
- Build with the IntelliJ Platform Gradle Plugin 2.x.
- The plugin depends on the bundled Terminal plugin: `org.jetbrains.plugins.terminal`.
- Keep IntelliJ API usage isolated. In particular, terminal API calls belong in `terminal/`.
- Keep managed runtime behavior aligned with `ite-vscode/extension.js` unless intentionally diverging.

## Commands

| Action | Command |
|---|---|
| Build/check | `./gradlew check` |
| Run dev IDE | `./gradlew runIde` |
| Package plugin zip | `./gradlew buildPlugin` |
| Sign plugin | `CERTIFICATE_CHAIN=... PRIVATE_KEY=... PRIVATE_KEY_PASSWORD=... ./gradlew signPlugin` |
| Publish plugin | `ORG_GRADLE_PROJECT_intellijPlatformPublishingToken=... ./gradlew publishPlugin` |

## Configuration

- Settings UI: `Settings | Tools | iTE`
- Managed install root:
  - macOS/Linux: `~/.ite`
  - Windows: `%LOCALAPPDATA%/iTE`
- Release manifest: `https://ite.kiishi.space/releases/manifest.json`
- Development sandbox: `build/<target-ide>/idea-sandbox`
- Signing secrets are read from environment variables; do not commit certificates, private keys, or Marketplace tokens.
