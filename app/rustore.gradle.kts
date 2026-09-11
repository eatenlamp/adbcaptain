// RuStore flavor, isolated from the F-Droid configuration.
// Applied only when building with -Prustore (see build.gradle.kts).
android {
    productFlavors {
        create("rustore") {
            dimension = "store"
            // Identical FOSS build for RuStore. No paid features, no analytics.
        }
    }
}