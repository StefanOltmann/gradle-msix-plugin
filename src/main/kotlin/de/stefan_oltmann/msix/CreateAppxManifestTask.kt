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

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

/**
 * Creates an AppxManifest.xml file from configured manifest properties.
 *
 * Resource paths and resource language are fixed in the template to keep the
 * manifest output aligned with created PNG resources.
 */
@CacheableTask
abstract class CreateAppxManifestTask : DefaultTask() {

    /**
     * Optional template file that overrides the built-in template.
     *
     * Example: "packaging/msix/AppxManifest.template.xml".
     * The template must keep the fixed logo paths and resource language
     * to stay compatible with the created resources.
     */
    @get:Optional
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val templateFile: RegularFileProperty

    /**
     * Output location for the created AppxManifest.xml.
     *
     * The MSIX packaging step reads this file from the app directory.
     */
    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    /**
     * Identity name for the MSIX package.
     *
     * Example: "StefanOltmann.MinesforWindowsPlus".
     */
    @get:Input
    abstract val identityName: Property<String>

    /**
     * Publisher string matching the signing certificate subject.
     *
     * Example: "CN=1A06AF6C-2943-4BE6-BB85-12677BA3F28D".
     */
    @get:Input
    abstract val publisher: Property<String>

    /**
     * Package version in four-part MSIX format.
     *
     * Example: "1.2.3.0".
     */
    @get:Input
    abstract val version: Property<String>

    /**
     * Processor architecture string, typically x64.
     *
     * Example: "x64".
     */
    @get:Input
    abstract val processorArchitecture: Property<String>

    /**
     * Display name shown in Windows.
     *
     * Example: "Mines+".
     */
    @get:Input
    abstract val displayName: Property<String>

    /**
     * Publisher display name shown in Windows.
     *
     * Example: "Stefan Oltmann".
     */
    @get:Input
    abstract val publisherDisplayName: Property<String>

    /**
     * Short description for VisualElements.
     *
     * Example: "Solvable Minesweeper".
     */
    @get:Input
    abstract val visualDescription: Property<String>

    /**
     * Background color for VisualElements.
     *
     * Example: "transparent".
     */
    @get:Input
    abstract val backgroundColor: Property<String>

    /**
     * Executable file name inside the app package.
     *
     * Example: "Mines.exe".
     */
    @get:Input
    abstract val appExecutable: Property<String>

    /**
     * Application ID within the MSIX package.
     *
     * Example: "App".
     */
    @get:Input
    abstract val appId: Property<String>

    /**
     * TargetDeviceFamily name.
     *
     * Example: "Windows.Desktop".
     */
    @get:Input
    abstract val targetDeviceFamilyName: Property<String>

    /**
     * Minimum Windows version supported.
     *
     * Example: "10.0.17763.0".
     */
    @get:Input
    abstract val targetDeviceFamilyMinVersion: Property<String>

    /**
     * Maximum Windows version tested.
     *
     * Example: "10.0.22621.0".
     */
    @get:Input
    abstract val targetDeviceFamilyMaxVersionTested: Property<String>

    /**
     * Writes the rendered AppxManifest.xml to the configured output file.
     */
    @TaskAction
    fun createManifest() {

        /*
         * Load the template and replace placeholders with configured values.
         */
        val template = loadTemplate()
        val manifestXml = applyTemplate(template)

        /*
         * Ensure the output directory exists before writing the manifest file.
         */
        val targetFile = outputFile.get().asFile
        targetFile.parentFile.mkdirs()
        targetFile.writeText(manifestXml)

        logger.lifecycle("Created AppxManifest.xml in ${targetFile.absolutePath}")
    }

    private fun loadTemplate(): String {

        if (templateFile.isPresent)
            return templateFile.get().asFile.readText()

        /*
         * Fall back to the default template bundled in the plugin resources.
         */
        val resourceUrl = javaClass.classLoader.getResource("AppxManifest.template.xml")
            ?: throw GradleException("Default AppxManifest template not found in plugin resources.")

        return resourceUrl.readText()
    }

    private fun applyTemplate(template: String): String {

        val values = mapOf(
            "identityName" to identityName.get(),
            "publisher" to publisher.get(),
            "version" to version.get(),
            "processorArchitecture" to processorArchitecture.get(),
            "displayName" to displayName.get(),
            "publisherDisplayName" to publisherDisplayName.get(),
            "description" to visualDescription.get(),
            "backgroundColor" to backgroundColor.get(),
            "appExecutable" to appExecutable.get(),
            "appId" to appId.get(),
            "targetDeviceFamilyName" to targetDeviceFamilyName.get(),
            "targetDeviceFamilyMinVersion" to targetDeviceFamilyMinVersion.get(),
            "targetDeviceFamilyMaxVersionTested" to targetDeviceFamilyMaxVersionTested.get()
        )

        /*
         * Replace placeholders in the template with the configured values.
         */
        var rendered = template

        values.forEach { (key, value) ->
            rendered = rendered.replace("{{${key}}}", value)
        }

        return rendered
    }
}
