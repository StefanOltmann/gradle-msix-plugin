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
 * Holds the manifest values required to render a valid AppxManifest.xml file.
 *
 * These values describe package identity, visual metadata, and target device
 * compatibility. Asset paths and resource language are fixed in the template,
 * so the configuration only captures the metadata that actually varies per app.
 */
abstract class MsixManifestExtension @Inject constructor(
    objects: ObjectFactory
) {

    /**
     * Optional template file that overrides the built-in template.
     *
     * The template must keep the fixed logo paths and resource language values.
     */
    val templateFile: RegularFileProperty = objects.fileProperty()

    /**
     * Identity name for the MSIX package.
     *
     * This should match the package identity registered in the Windows Store.
     */
    val identityName: Property<String> = objects.property(String::class.java)

    /**
     * Publisher string matching the signing certificate subject.
     *
     * If signing is enabled, the PFX certificate subject must match this value.
     */
    val publisher: Property<String> = objects.property(String::class.java)

    /**
     * Package version in a four-part MSIX format (e.g. "1.2.3.0").
     *
     * Windows requires exactly four numeric segments.
     */
    val version: Property<String> = objects.property(String::class.java)

    /**
     * Processor architecture string, typically "x64".
     *
     * This is also used to resolve makeappx.exe and signtool.exe.
     */
    val processorArchitecture: Property<String> = objects.property(String::class.java)

    /**
     * Display name shown.
     *
     * This is the public-facing name shown in Start and app lists.
     */
    val displayName: Property<String> = objects.property(String::class.java)

    /** Publisher display name shown. */
    val publisherDisplayName: Property<String> = objects.property(String::class.java)

    /**
     * Short description for VisualElements.
     *
     * This text appears when users view app details.
     */
    val description: Property<String> = objects.property(String::class.java)

    /**
     * Background color for VisualElements.
     *
     * Use "transparent" to keep the tile background unobtrusive.
     */
    val backgroundColor: Property<String> = objects.property(String::class.java)

    /**
     * Executable file name inside the app package.
     *
     * This must match the executable created by Compose Desktop.
     */
    val appExecutable: Property<String> = objects.property(String::class.java)

    /**
     * Application ID within the MSIX package.
     *
     * This value identifies the application element inside the manifest.
     */
    val appId: Property<String> = objects.property(String::class.java)

    /** TargetDeviceFamily name. */
    val targetDeviceFamilyName: Property<String> = objects.property(String::class.java)

    /** Minimum Windows version supported. */
    val targetDeviceFamilyMinVersion: Property<String> = objects.property(String::class.java)

    /** Maximum Windows version tested. */
    val targetDeviceFamilyMaxVersionTested: Property<String> = objects.property(String::class.java)

}
