// SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
// SPDX-License-Identifier: AGPL-3.0-or-later
//
// ADB Captain. Ownership notice - do not remove.
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlinCompose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlinAndroid) apply false
}
