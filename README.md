# iTE IntelliJ Plugin

Run iTE in a dedicated JetBrains IDE terminal.

This plugin mirrors the iTE VS Code extension:

- Open iTE from `Tools | iTE` or Find Action.
- Start a fresh iTE terminal.
- Resume the most recent saved iTE session.
- Check whether the iTE runtime is available.
- Install the official iTE runtime from `ite.kiishi.space`.

## Development

```sh
./gradlew runIde
./gradlew check
./gradlew buildPlugin
```

Settings live under `Settings | Tools | iTE`.

The `runIde` development sandbox is created under `build/<target-ide>/idea-sandbox`.
Delete that sandbox directory when you need to reset the development IDE state.

## Signing and Publishing

Signing uses environment variables:

- `CERTIFICATE_CHAIN`
- `PRIVATE_KEY`
- `PRIVATE_KEY_PASSWORD`

Publishing uses Gradle properties:

- `intellijPlatformPublishingToken`
- `pluginChannels`

Example:

```sh
ORG_GRADLE_PROJECT_intellijPlatformPublishingToken=... ./gradlew publishPlugin
```
