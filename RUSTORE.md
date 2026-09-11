# RuStore build (separate)

The main project config is a single pure-FOSS `fdroid` flavor. The RuStore build is isolated in `app/rustore.gradle.kts` and is only activated on demand, so it never appears in the F-Droid-facing configuration.

The RuStore variant is identical FOSS: all features, no paid tiers, no analytics, no donation links.

## Build

```bash
./gradlew assembleRustoreDebug -Prustore

# Signed release build (requires keystore.properties, see README.md)
./gradlew assembleRustoreRelease -Prustore
```

## Why isolated

Keeping the RuStore flavor out of the default build graph avoids store references in the F-Droid metadata and keeps the main Gradle config clean for review.