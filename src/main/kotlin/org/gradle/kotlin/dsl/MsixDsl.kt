/*
 * Gradle MSIX Plugin
 * Copyright (C) 2026 Stefan Oltmann
 * https://github.com/StefanOltmann/gradle-msix-plugin
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
@file:Suppress("unused")

package org.gradle.kotlin.dsl

import de.stefan_oltmann.msix.MsixExtension
import de.stefan_oltmann.msix.MsixManifestExtension
import org.gradle.api.Action
import org.gradle.api.Project

/**
 * Configures the MSIX plugin extension using the `msix {}` DSL block.
 *
 * This keeps build scripts concise while still giving access to the manifest
 * metadata and optional signing configuration required for MSIX packaging.
 */
fun Project.msix(configuration: MsixExtension.() -> Unit) =
    extensions.configure(
        MsixExtension::class.java,
        Action { extension -> extension.configuration() }
    )

/**
 * Configures the MSIX manifest values inside the `msix {}` block.
 */
fun MsixExtension.manifest(configuration: MsixManifestExtension.() -> Unit) =
    manifest.configuration()
