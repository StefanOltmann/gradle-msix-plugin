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

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.gradle.testkit.runner.GradleRunner

/**
 * Pins how the plugin resolves the distribution name that every packaging path is laid out
 * under (`build/compose/binaries/main-release/app/<name>`).
 *
 * The implicit resolutions (Compose extension reflection, output directory listing) depend on
 * what exists at realization time - on a fresh checkout nothing does, so the project name won
 * and the manifest was rendered into a directory the Compose distribution never uses. The
 * explicit `packageName` property makes the layout deterministic; these tests run real Gradle
 * builds through TestKit to pin both the property and the final fallback.
 */
class MsixPackageNameResolutionTest {

    private lateinit var projectDir: Path

    @BeforeTest
    fun createConsumerProject() {

        projectDir = Files.createTempDirectory("msix-package-name")

        Files.writeString(
            projectDir.resolve("settings.gradle.kts"),
            "rootProject.name = \"consumer\"\n"
        )
    }

    @AfterTest
    fun deleteConsumerProject() {
        projectDir.toFile().deleteRecursively()
    }

    @Test
    fun testTheConfiguredPackageNameDeterminesTheLayoutDirectory() {

        writeConsumerBuild(
            """
            packageName.set("PixRater")
            $manifestValues
            """.trimIndent()
        )

        runCreateAppxManifest()

        val layoutRoot = projectDir.resolve("build/compose/binaries/main-release/app")

        assertTrue(
            layoutRoot.resolve("PixRater/AppxManifest.xml").exists(),
            "The manifest must be rendered into the directory named after the configured " +
                "package name"
        )

        assertFalse(
            layoutRoot.resolve("consumer/AppxManifest.xml").exists(),
            "The project-name fallback must not win over the configured package name"
        )
    }

    @Test
    fun testWithoutAnyResolutionInputTheProjectNameIsTheFinalFallback() {

        writeConsumerBuild(manifestValues)

        runCreateAppxManifest()

        assertTrue(
            projectDir.resolve("build/compose/binaries/main-release/app/consumer/AppxManifest.xml").exists(),
            "With no configured name, no Compose extension, and no built distribution, the " +
                "Gradle project name is the last resolution step"
        )
    }

    private fun writeConsumerBuild(msixBlock: String) {

        Files.writeString(
            projectDir.resolve("build.gradle.kts"),
            """
            plugins {
                id("de.stefan-oltmann.gradle-msix-plugin")
            }

            msix {
            $msixBlock
            }
            """.trimIndent() + "\n"
        )
    }

    private fun runCreateAppxManifest() {

        GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withPluginClasspath()
            .withArguments("createAppxManifest")
            .build()
    }

    private companion object {

        /*
         * The manifest properties without conventions - every one must be set before
         * createAppxManifest can render.
         */
        val manifestValues = """
            manifest {
                appId.set("App")
                displayName.set("App")
                identityName.set("Example.App")
                publisher.set("CN=Example")
                publisherDisplayName.set("Example")
                description.set("Test")
                appExecutable.set("App.exe")
                version.set("1.0.0.0")
            }
        """.trimIndent()
    }
}
