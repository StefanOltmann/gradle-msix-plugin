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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider

/**
 * Gradle plugin that wires MSIX resource creation, manifest creation, and packaging tasks.
 *
 * The plugin keeps output locations and resource names aligned with the standard
 * Compose Desktop release layout to minimize configuration overhead.
 */
@Suppress("unused")
class MsixPlugin : Plugin<Project> {

    /**
     * Registers the msix extension, conventions, and tasks for MSIX packaging.
     */
    override fun apply(project: Project) {

        val extension = project.extensions.create("msix", MsixExtension::class.java)

        /*
         * Conventions keep configuration minimal while producing a valid manifest.
         */
        extension.svgIcon.convention(project.layout.projectDirectory.file("packaging/msix/resources/AppIcon.svg"))
        extension.manifest.processorArchitecture.convention("x64")
        extension.manifest.backgroundColor.convention("transparent")
        extension.manifest.targetDeviceFamilyName.convention("Windows.Desktop")
        extension.manifest.targetDeviceFamilyMinVersion.convention("10.0.17763.0")
        extension.manifest.targetDeviceFamilyMaxVersionTested.convention("10.0.22621.2861")

        /*
         * Resolve the Compose Desktop output layout at once and share it across tasks.
         */
        val packageNameProvider = project.providers.provider { resolvePackageName(project) }

        val appRoot = project.layout.buildDirectory.dir("compose/binaries/main-release/app")

        val appDirectory = appRoot.flatMap { root ->
            packageNameProvider.map { root.dir(it) }
        }

        val resourcesDir = appDirectory.map { it.dir("resources") }
        val manifestFile = appDirectory.map { it.file("AppxManifest.xml") }
        val msixOutputFile = project.layout.buildDirectory.file(packageNameProvider.map { "$it.msix" })

        /*
         * Render the fixed PNG resource set from the configured SVG.
         */
        val createIcons = project.tasks.register("createMsixIcons", CreateMsixIconsTask::class.java) { task ->
            task.svgIcon.set(extension.svgIcon)
            task.outputDir.set(resourcesDir)
            task.group = "msix"
            task.description = "Create MSIX PNG resources from the configured SVG."
        }

        /*
         * Create the manifest using the configured metadata and fixed resources.
         */
        val createManifest =
            project.tasks.register("createAppxManifest", CreateAppxManifestTask::class.java) { task ->
                task.templateFile.set(extension.manifest.templateFile)
                task.outputFile.set(manifestFile)
                task.identityName.set(extension.manifest.identityName)
                task.publisher.set(extension.manifest.publisher)
                task.version.set(extension.manifest.version)
                task.processorArchitecture.set(extension.manifest.processorArchitecture)
                task.displayName.set(extension.manifest.displayName)
                task.publisherDisplayName.set(extension.manifest.publisherDisplayName)
                task.visualDescription.set(extension.manifest.description)
                task.backgroundColor.set(extension.manifest.backgroundColor)
                task.appExecutable.set(extension.manifest.appExecutable)
                task.appId.set(extension.manifest.appId)
                task.targetDeviceFamilyName.set(extension.manifest.targetDeviceFamilyName)
                task.targetDeviceFamilyMinVersion.set(extension.manifest.targetDeviceFamilyMinVersion)
                task.targetDeviceFamilyMaxVersionTested.set(extension.manifest.targetDeviceFamilyMaxVersionTested)
                task.group = "msix"
                task.description = "Create AppxManifest.xml for MSIX packaging."
            }

        /*
         * Package the app directory into the final MSIX file.
         */
        val createMsix = project.tasks.register("createMsix", CreateMsixTask::class.java) { task ->
            task.appDirectory.set(appDirectory)
            task.msixOutputFile.set(msixOutputFile)
            task.processorArchitecture.set(extension.manifest.processorArchitecture)
            task.signingPfxFile.set(extension.signingPfx)
            task.signingPassword.set(extension.signingPassword)
            task.dependsOn(createIcons, createManifest)
            task.group = "msix"
            task.description = "Build and optionally sign an MSIX package using makeappx.exe."
        }

        /*
         * Ensure the Compose release distributable is created before packaging runs.
         */
        val releaseTaskName = "createReleaseDistributable"

        createMsix.configure { it.dependsOn(releaseTaskName) }
        createIcons.configure { it.mustRunAfter(releaseTaskName) }
        createManifest.configure { it.mustRunAfter(releaseTaskName) }
    }

    /**
     * Resolves the package name from Compose configuration or falls back
     * to the Gradle project name.
     */
    private fun resolvePackageName(project: Project): String {

        val composeName = findComposePackageName(project)?.takeIf { it.isNotBlank() }

        if (composeName != null)
            return composeName

        val outputName = findPackageNameFromOutput(project)?.takeIf { it.isNotBlank() }
        return outputName ?: project.name
    }

    /**
     * Attempts to read the Compose Desktop native distribution package name.
     *
     * Reflection is used to avoid compile-time coupling to Compose internals.
     */
    private fun findComposePackageName(project: Project): String? {

        val composeExtension = project.extensions.findByName("compose") ?: return null

        return runCatching {

            val desktop = composeExtension.javaClass.methods.firstOrNull { it.name == "getDesktop" }
                ?.invoke(composeExtension) ?: return@runCatching null

            val application = desktop.javaClass.methods.firstOrNull { it.name == "getApplication" }
                ?.invoke(desktop) ?: return@runCatching null

            val nativeDistributions = application.javaClass.methods
                .firstOrNull { it.name == "getNativeDistributions" }
                ?.invoke(application) ?: return@runCatching null

            val packageNameValue = nativeDistributions.javaClass.methods
                .firstOrNull { it.name == "getPackageName" }
                ?.invoke(nativeDistributions)

            extractProviderValue(packageNameValue)

        }.getOrNull()
    }

    /**
     * Extracts a value from a Gradle Provider without forcing resolution
     * when the provider is absent.
     */
    private fun extractProviderValue(value: Any?): String? {
        return when (value) {
            is Provider<*> -> value.orNull?.toString()
            null -> null
            else -> value.toString()
        }
    }

    /**
     * Falls back to the first app output folder when Compose configuration
     * is unavailable, which can happen in composite builds.
     */
    private fun findPackageNameFromOutput(project: Project): String? {

        val outputRoot = project.layout.buildDirectory.dir("compose/binaries/main-release/app").get().asFile

        if (!outputRoot.exists())
            return null

        val candidates = outputRoot.listFiles()
            ?.filter { it.isDirectory }
            .orEmpty()

        if (candidates.size == 1)
            return candidates.first().name

        return candidates.firstOrNull { it.name.equals(project.name, ignoreCase = true) }?.name
    }
}
