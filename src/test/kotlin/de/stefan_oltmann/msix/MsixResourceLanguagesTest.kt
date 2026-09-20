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
import kotlin.io.path.readText
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.gradle.testkit.runner.GradleRunner

/**
 * Pins how the default manifest template declares resource languages. Multi-language apps
 * must ship one `<Resource Language="..."/>` element per language, so the plugin takes the
 * languages from configuration and fills the template's `{{resourceLanguages}}` placeholder
 * - a custom template without the placeholder keeps declaring its resources itself.
 */
class MsixResourceLanguagesTest {

    private lateinit var projectDir: Path

    @BeforeTest
    fun createConsumerProject() {

        projectDir = Files.createTempDirectory("msix-resource-languages")

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
    fun testTheDefaultManifestDeclaresEnglishOnlyByDefault() {

        writeConsumerBuild(manifestProperties)

        runCreateAppxManifest()

        val manifest = readRenderedManifest()

        assertTrue(
            "<Resource Language=\"en\"/>" in manifest,
            "The default language set declares English"
        )

        assertFalse(
            "Language=\"de\"" in manifest,
            "No other language is declared without configuration"
        )
    }

    @Test
    fun testTheConfiguredLanguagesBecomeOneResourceElementEach() {

        writeConsumerBuild(
            """
            languages.set(listOf("en", "de"))
            $manifestProperties
            """.trimIndent()
        )

        runCreateAppxManifest()

        val manifest = readRenderedManifest()

        assertTrue(
            "<Resource Language=\"en\"/>" in manifest,
            "The first configured language must be declared"
        )

        assertTrue(
            "<Resource Language=\"de\"/>" in manifest,
            "Every configured language must get its own Resource element - a language " +
                "missing from the manifest is invisible to the Store's language listing"
        )
    }

    private fun writeConsumerBuild(manifestProperties: String) {

        Files.writeString(
            projectDir.resolve("build.gradle.kts"),
            """
            plugins {
                id("de.stefan-oltmann.gradle-msix-plugin")
            }

            msix {
                manifest {
                $manifestProperties
                }
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

    private fun readRenderedManifest(): String =
        projectDir
            .resolve("build/compose/binaries/main-release/app/consumer/AppxManifest.xml")
            .readText()

    private companion object {

        /*
         * The manifest properties without conventions - every one must be set before
         * createAppxManifest can render. No packageName: the project-name fallback keeps
         * the layout directory predictable for these tests.
         */
        val manifestProperties = """
            appId.set("App")
            displayName.set("App")
            identityName.set("Example.App")
            publisher.set("CN=Example")
            publisherDisplayName.set("Example")
            description.set("Test")
            appExecutable.set("App.exe")
            version.set("1.0.0.0")
        """.trimIndent()
    }
}
