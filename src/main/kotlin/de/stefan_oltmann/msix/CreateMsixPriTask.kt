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
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

/**
 * Generates resources.pri by indexing MSIX resources with makepri.exe.
 */
abstract class CreateMsixPriTask @Inject constructor(
    private val execOperations: ExecOperations
) : DefaultTask() {

    /**
     * Windows SDK install instructions shown when required tools are missing.
     */
    private val windowsSdkInstallInstructions = """
        Download the Windows SDK from https://learn.microsoft.com/de-de/windows/apps/windows-sdk/downloads
        Then run: winsdksetup.exe /features OptionId.DesktopCPPx64 /quiet /norestart
        Confirm the UAC dialog and wait a minute while the installation takes place; then try again.
    """.trimIndent()

    /**
     * Directory that contains the prepared MSIX contents.
     */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val appDirectory: DirectoryProperty

    /**
     * AppxManifest.xml used to describe resources.
     */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val manifestFile: RegularFileProperty

    /**
     * Processor architecture used to locate the correct Windows SDK binary.
     */
    @get:Input
    abstract val processorArchitecture: Property<String>

    /**
     * Output resources.pri file created by makepri.exe.
     */
    @get:OutputFile
    abstract val priFile: RegularFileProperty

    /**
     * Intermediate PRI config file created by makepri.exe.
     */
    @get:OutputFile
    abstract val priConfigFile: RegularFileProperty

    /**
     * Builds resources.pri using makepri.exe.
     */
    @TaskAction
    fun buildPri() {

        if (!isWindows()) {
            logger.lifecycle("Skipping PRI generation on non-Windows host.")
            return
        }

        val appDir = appDirectory.get().asFile

        if (!appDir.exists())
            throw GradleException("MSIX app directory not found at ${appDir.absolutePath}")

        val manifest = manifestFile.get().asFile

        if (!manifest.exists())
            throw GradleException("AppxManifest.xml not found at ${manifest.absolutePath}")

        val makePriFile = resolveMakePri()

        val configFile = priConfigFile.get().asFile
        configFile.parentFile.mkdirs()

        if (configFile.exists()) {
            if (!configFile.delete())
                throw GradleException("Unable to delete existing PRI config at ${configFile.absolutePath}")
        }

        logger.lifecycle("Using makepri.exe at ${makePriFile.absolutePath}")
        logger.lifecycle("Generating PRI config at ${configFile.absolutePath}")
        logger.lifecycle("Running: ${makePriFile.absolutePath} createconfig /cf ${configFile.absolutePath} /dq en-us")

        execOperations.exec { spec ->
            spec.workingDir(makePriFile.parentFile)
            spec.commandLine(
                makePriFile.absolutePath,
                "createconfig",
                "/cf",
                configFile.absolutePath,
                "/dq",
                "en-us"
            )
            spec.standardOutput = System.out
            spec.errorOutput = System.err
        }

        val priOutput = priFile.get().asFile
        priOutput.parentFile.mkdirs()

        if (priOutput.exists()) {
            if (!priOutput.delete())
                throw GradleException("Unable to delete existing resources.pri at ${priOutput.absolutePath}")
        }

        logger.lifecycle("Creating resources.pri at ${priOutput.absolutePath}")
        logger.lifecycle("Running: ${makePriFile.absolutePath} new /pr ${appDir.absolutePath} /cf ${configFile.absolutePath} /of ${priOutput.absolutePath}")

        execOperations.exec { spec ->
            spec.workingDir(makePriFile.parentFile)
            spec.commandLine(
                makePriFile.absolutePath,
                "new",
                "/pr",
                appDir.absolutePath,
                "/cf",
                configFile.absolutePath,
                "/of",
                priOutput.absolutePath
            )
            spec.standardOutput = System.out
            spec.errorOutput = System.err
        }

        logger.lifecycle("Generated resources.pri at ${priOutput.absolutePath}")
    }

    private fun resolveMakePri(): File {

        val architecture = processorArchitecture.get()

        return WindowsKitsLocator.locateMakePri(architecture)
            ?: throw GradleException(
                "makepri.exe not found.\n" +
                    windowsSdkInstallInstructions
            )
    }

    private fun isWindows(): Boolean =
        System.getProperty("os.name").startsWith("Windows")
}
