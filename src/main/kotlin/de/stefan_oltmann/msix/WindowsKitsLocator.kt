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

import java.io.File

/**
 * Helper for locating Windows SDK tools such as makeappx.exe and signtool.exe.
 *
 * The SDK layout is scanned using the configured architecture, preferring the
 * newest installed version when multiple versions are present.
 */
internal object WindowsKitsLocator {

    private val versionRegex = Regex("""\d+(\.\d+){3}""")

    /**
     * Locates makeappx.exe for the requested architecture.
     * Returns null when it cannot be found.
     */
    fun locateMakeAppx(architecture: String): File? =
        locateTool("makeappx.exe", architecture)

    /**
     * Locates signtool.exe for the requested architecture.
     * Returns null when it cannot be found.
     */
    fun locateSignTool(architecture: String): File? =
        locateTool("signtool.exe", architecture)

    /**
     * Locates a tool in the Windows Kits "bin" directory hierarchy.
     */
    private fun locateTool(toolName: String, architecture: String): File? {

        val kitsRoot = findWindowsKitsRoot() ?: return null

        val binDir = File(kitsRoot, "bin")

        val directCandidate = File(File(binDir, architecture), toolName)

        if (directCandidate.exists())
            return directCandidate

        val versionDirs = findVersionDirectories(binDir)

        if (versionDirs.isEmpty())
            return null

        val sortedVersions = versionDirs.sortedWith { left, right ->
            compareVersions(parseVersion(right.name), parseVersion(left.name))
        }

        for (versionDir in sortedVersions) {

            val candidate = File(File(versionDir, architecture), toolName)

            if (candidate.exists())
                return candidate
        }

        return null
    }

    /**
     * Looks for the Windows Kits root folder under Program Files.
     */
    private fun findWindowsKitsRoot(): File? {

        val programFiles = System.getenv("ProgramFiles(x86)") ?: System.getenv("ProgramFiles") ?: return null

        val candidates = listOf("Windows Kits/10", "Windows Kits/11")
            .map { File(programFiles, it) }

        return candidates.firstOrNull { it.exists() }
    }

    /**
     * Filters bin subfolders that follow a Windows SDK version pattern.
     */
    private fun findVersionDirectories(binDir: File): List<File> =
        binDir.listFiles()
            ?.filter { it.isDirectory && versionRegex.matches(it.name) }
            .orEmpty()

    /**
     * Parses a dotted version string into a list of numeric components.
     */
    private fun parseVersion(version: String): List<Int> =
        version.split('.').mapNotNull { it.toIntOrNull() }

    /**
     * Compares version lists so higher versions sort first.
     */
    private fun compareVersions(left: List<Int>, right: List<Int>): Int {

        val maxSize = maxOf(left.size, right.size)

        for (index in 0 until maxSize) {

            val leftValue = left.getOrElse(index) { 0 }
            val rightValue = right.getOrElse(index) { 0 }

            if (leftValue != rightValue)
                return leftValue.compareTo(rightValue)
        }

        return 0
    }
}
