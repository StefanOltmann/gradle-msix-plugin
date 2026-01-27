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
package de.stefan_oltmann.msix

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Top-level configuration for the MSIX Gradle plugin.
 *
 * The public surface is intentionally small: it exposes only the SVG icon input,
 * the manifest values required to describe the package contents, and the optional
 * signing PFX file that enables package signing.
 */
abstract class MsixExtension @Inject constructor(
    objects: ObjectFactory
) {

    /**
     * Path to the SVG icon that will be rendered into fixed MSIX PNG resources.
     *
     * The plugin renders fixed sizes that match the hardcoded manifest entries.
     */
    val svgIcon: RegularFileProperty = objects.fileProperty()

    /**
     * Optional PFX file used to sign the created MSIX package.
     *
     * Example: "packaging/msix/sign.pfx".
     *
     * Leave this unset to skip signing.
     * When set, the plugin will invoke signtool.exe after packaging.
     */
    val signingPfx: RegularFileProperty = objects.fileProperty()

    /**
     * Optional password for the configured PFX file.
     *
     * Example: "securePassword".
     *
     * This must be set when a PFX file is configured, otherwise signing fails.
     */
    val signingPassword: Property<String> = objects.property(String::class.java)

    /** Manifest metadata used to create the AppxManifest.xml file. */
    val manifest: MsixManifestExtension = objects.newInstance(MsixManifestExtension::class.java)
}
