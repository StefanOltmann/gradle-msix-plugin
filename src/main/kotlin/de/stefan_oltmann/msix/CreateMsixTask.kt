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
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

/**
 * Runs makeappx.exe to create an MSIX package from the configured app directory.
 *
 * The makeappx.exe location is resolved automatically from the installed
 * Windows SDK for the requested architecture. Optional signing is performed
 * with signtool.exe when a PFX file is provided.
 */
abstract class CreateMsixTask @Inject constructor(
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
     *
     * Example: "build/compose/binaries/main-release/app/Mines".
     *
     * The directory must already include the manifest and resources.
     */
    @get:InputDirectory
    abstract val appDirectory: DirectoryProperty

    /**
     * Target MSIX output file.
     *
     * Example: "build/Mines.msix".
     *
     * The packaged MSIX is written here before optional signing is applied.
     */
    @get:OutputFile
    abstract val msixOutputFile: RegularFileProperty

    /**
     * Processor architecture used to locate the correct Windows SDK binary.
     */
    @get:Input
    abstract val processorArchitecture: Property<String>

    /**
     * Optional PFX file used to sign the created MSIX package.
     *
     * Example: "packaging/msix/sign.pfx".
     *
     * When not present, the MSIX is left unsigned.
     */
    @get:Optional
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val signingPfxFile: RegularFileProperty

    /**
     * Optional password for the configured PFX file.
     *
     * Example: "test".
     * This must be provided when a PFX file is configured.
     */
    @get:Optional
    @get:Input
    abstract val signingPassword: Property<String>

    /**
     * Packs the MSIX using makeappx.exe if the host OS is Windows.
     */
    @TaskAction
    fun pack() {

        if (!isWindows()) {
            logger.lifecycle("Skipping MSIX packaging on non-Windows host.")
            return
        }

        /*
         * Ensure the app directory exists before invoking makeappx.exe.
         */
        val appDir = appDirectory.get().asFile

        if (!appDir.exists())
            throw GradleException("MSIX app directory not found at ${appDir.absolutePath}")

        val makeAppxFile = resolveMakeAppx()

        val target = msixOutputFile.get().asFile
        target.parentFile.mkdirs()

        logger.lifecycle("Using makeappx.exe at ${makeAppxFile.absolutePath}")

        if (target.exists()) {

            logger.lifecycle("Removing existing MSIX output before packaging: ${target.absolutePath}")

            if (!target.delete())
                throw GradleException("Unable to delete existing MSIX output at ${target.absolutePath}")
        }

        packMsix(makeAppxFile, appDir, target)
        signIfConfigured(target)
    }

    private fun packMsix(makeAppxFile: File, appDir: File, target: File) {

        /*
         * The makeappx.exe command packages the prepared app directory.
         */
        logger.lifecycle("Packaging MSIX from ${appDir.absolutePath} into ${target.absolutePath}")
        logger.lifecycle("Running: ${makeAppxFile.absolutePath} pack /d ${appDir.absolutePath} /p ${target.absolutePath} /o")

        execOperations.exec { spec ->
            spec.workingDir(makeAppxFile.parentFile)
            spec.commandLine(
                makeAppxFile.absolutePath,
                "pack",
                "/d",
                appDir.absolutePath,
                "/p",
                target.absolutePath,
                "/o"
            )
            spec.standardOutput = System.out
            spec.errorOutput = System.err
        }

        logger.lifecycle("MSIX package created at ${target.absolutePath}")
    }

    private fun signIfConfigured(target: File) {

        if (!signingPfxFile.isPresent) {
            logger.lifecycle("Skipping MSIX signing because no PFX file was configured.")
            return
        }

        val password = signingPassword.orNull?.takeIf { it.isNotBlank() }
            ?: throw GradleException("MSIX signing is enabled but msix.signingPassword is missing. Provide the PFX password.")

        val pfxFile = signingPfxFile.get().asFile

        if (!pfxFile.exists())
            throw GradleException("Signing PFX file not found at ${pfxFile.absolutePath}")

        val signTool = resolveSignTool()

        logger.lifecycle("Using signtool.exe at ${signTool.absolutePath}")

        /*
         * Sign the created MSIX package using the provided PFX file.
         */
        logger.lifecycle("Signing MSIX package with ${pfxFile.absolutePath}")
        logger.lifecycle("Running: ${signTool.absolutePath} sign /fd SHA256 /f ${pfxFile.absolutePath} /p <redacted> ${target.absolutePath}")

        execOperations.exec { spec ->
            spec.workingDir(signTool.parentFile)
            spec.commandLine(
                signTool.absolutePath,
                "sign",
                "/fd",
                "SHA256",
                "/f",
                pfxFile.absolutePath,
                "/p",
                password,
                target.absolutePath
            )
            spec.standardOutput = System.out
            spec.errorOutput = System.err
        }

        logger.lifecycle("Signed MSIX package at ${target.absolutePath}")
    }

    private fun resolveMakeAppx(): File {

        val architecture = processorArchitecture.get()

        return WindowsKitsLocator.locateMakeAppx(architecture)
            ?: throw GradleException(
                "makeappx.exe not found.\n" +
                    windowsSdkInstallInstructions
            )
    }

    private fun resolveSignTool(): File {

        val architecture = processorArchitecture.get()

        return WindowsKitsLocator.locateSignTool(architecture)
            ?: throw GradleException(
                "signtool.exe not found.\n" +
                    windowsSdkInstallInstructions
            )
    }

    private fun isWindows(): Boolean =
        System.getProperty("os.name").startsWith("Windows")
}
