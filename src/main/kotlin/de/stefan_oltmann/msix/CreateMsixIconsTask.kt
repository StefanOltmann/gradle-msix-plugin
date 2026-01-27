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

import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import org.apache.batik.transcoder.image.PNGTranscoder
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import java.io.FileOutputStream

/**
 * Creates the fixed set of MSIX PNG resources from a single SVG source file.
 *
 * The output sizes and filenames are internal to the plugin to ensure
 * consistent manifest references and avoid configuration drift.
 */
@CacheableTask
abstract class CreateMsixIconsTask : DefaultTask() {

    /**
     * SVG file used as the source for rendering PNGs.
     *
     * Example: "packaging/msix/resources/AppIcon.svg"
     *
     * The SVG should be a square icon, so scaling retains expected proportions.
     */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val svgIcon: RegularFileProperty

    /**
     * Directory where PNG files are written.
     *
     * This directory is placed inside the MSIX app folder so the manifest
     * can reference the PNG resources directly.
     */
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    /**
     * Renders the PNG resources required by the MSIX manifest.
     */
    @TaskAction
    fun renderIcons() {

        /*
         * Validate inputs and ensure the output directory exists before rendering.
         */
        val svgFile = svgIcon.get().asFile

        if (!svgFile.exists())
            throw GradleException("SVG icon not found at ${svgFile.absolutePath}")

        val outputRoot = outputDir.get().asFile
        outputRoot.mkdirs()

        /*
         * Render every required MSIX icon size using fixed filenames
         * that align with the manifest template.
         */
        setOf(44, 50, 150).forEach { size ->
            val targetFile = outputRoot.resolve("icon_$size.png")
            renderSvgToPng(svgFile, targetFile, size)
        }

        logger.lifecycle("Rendered PNG resources in ${outputRoot.absolutePath}")
    }

    private fun renderSvgToPng(svgFile: java.io.File, targetFile: java.io.File, size: Int) {

        /*
         * Batik renders the SVG into a PNG at the requested dimensions.
         */
        val transcoder = PNGTranscoder()

        transcoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, size.toFloat())
        transcoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, size.toFloat())

        svgFile.inputStream().use { inputStream ->
            FileOutputStream(targetFile).use { outputStream ->
                val input = TranscoderInput(inputStream)
                val output = TranscoderOutput(outputStream)
                transcoder.transcode(input, output)
            }
        }
    }
}
