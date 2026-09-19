// RuStore flavor, isolated from the F-Droid configuration.
// Applied only when building with -Prustore (see build.gradle.kts).
android {
    productFlavors {
        create("rustore") {
            dimension = "store"
            // Identical FOSS build for RuStore. No paid features, no analytics.
            // Unique package so it can be installed alongside the F-Droid build.
            applicationId = "adb.captain.rustore"
            // RuStore variant is localized for a Russian audience: bundle only
            // Russian resources and treat them as the default (base) language.
            resourceConfigurations += "ru"
        }
    }
}