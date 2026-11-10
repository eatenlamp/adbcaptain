// SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
// SPDX-License-Identifier: AGPL-3.0-or-later
//
// ADB Captain. Ownership notice - do not remove.
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "ADB Captain"
include(":app")
 