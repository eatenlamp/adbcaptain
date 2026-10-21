# RuStore build

The project ships two product flavors from one source tree:

| Flavor | Application id | Signing key | Purpose |
|---|---|---|---|
| `fdroid` | `adb.captain` | project release key | F-Droid and GitHub Releases. Pure FOSS, no store library, no limits. |
| `rustore` | `adb.captain.rustore` | separate RuStore key | RuStore. Same features, plus the paid tier. |

Both flavors are always part of the build graph. The F-Droid metadata
(`fdroid/adb.captain.yml`) builds the `fdroid` flavor only, so the store build
never reaches F-Droid and the FOSS APK stays free of any store dependency.

```bash
./gradlew assembleFdroidRelease
./gradlew assembleRustoreRelease
```

## Paid tier

The store build can sell a Pro version through the billing client of the store.
The FOSS build has no store library at all: `app/src/fdroid` provides a no-op
implementation, and `STORE_FEATURES_ENABLED` is `false` there, so nothing is
limited. The store build sets that flag to `true`, which turns on the limits.

What Pro unlocks today:

- **Unlimited macros.** The free build keeps three.
- **Export a terminal session as Markdown**, for pasting into a bug report.

Both are listed in `adb.captain.domain.model.ProFeature`. Adding another paid
feature means adding a case there and one check where it matters, nothing else.

### Setup in the store console

1. Create the product in the RuStore console and enable in-app purchases.
2. Note the console application id and the product id.
3. Put them into `gradle.properties` (or pass them on the command line):

```properties
rustoreConsoleApplicationId=123456
rustoreProductIds=pro
rustoreDeeplinkScheme=adbcaptainrustore
rustoreDeveloperPayload=adb-captain
```

The deeplink scheme has to match the intent filter in
`app/src/rustore/AndroidManifest.xml`.

### Building with the billing client

The billing client lives on a maven repository of the store, so the build needs
access to it:

```kotlin
// settings.gradle.kts
maven { url = uri("https://artifactory-external.vkpartner.ru/artifactory/maven") }
```

The repository answers with 404 for requests that do not come from Russia. On a
network that cannot reach it, the store flavor cannot be built.

### Donation button

The store build shows a "support the developer" button in the settings. It opens
the listing of the app in the store, where the donation button of the store page
is configured in the console. The FOSS build has neither the button nor the
strings behind it.
