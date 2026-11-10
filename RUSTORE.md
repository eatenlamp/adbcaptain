# RuStore build

The project ships two product flavors from one source tree:

| Flavor | Application id | Signing key | Purpose |
|---|---|---|---|
| `fdroid` | `adb.captain` | project release key | F-Droid and GitHub Releases. Pure FOSS. |
| `rustore` | `adb.captain.rustore` | separate RuStore key | RuStore. Same features, no billing client. |

Both flavors are always part of the build graph. The F-Droid metadata
(`fdroid/adb.captain.yml`) builds the `fdroid` flavor only, so the store build
never reaches F-Droid.

```bash
./gradlew assembleFdroidRelease
./gradlew assembleRustoreRelease
```

## No paid tier

ADB Captain has no Pro version and no in-app purchases. Every feature,
including unlimited macros and the Markdown terminal export, is free in both
builds. The `rustore` flavor differs only in the application id and signing
key, so both APKs can be installed side by side.

The project intentionally has no store billing dependency: RuStore BillingClient
is deprecated, and shipping a purchase flow for features that are now free would
only add weight and tracking. There is also no donation button in the app; the
store page's own "support the developer" button is configured entirely in the
RuStore console.

## Building

No extra maven repository, no `gradle.properties` keys and no store console
setup are required. The store flavor builds offline with the same dependencies
as the FOSS flavor.
