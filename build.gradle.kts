plugins {
    id("java-gradle-plugin")
    id("maven-publish")
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.plugin.publish)
}

/* Note: Group must have a hyphen to match plugin id */
group = "de.stefan-oltmann"
version = "0.3.0"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.batik.transcoder)
    implementation(libs.batik.codec)

    testImplementation(gradleTestKit())
    testImplementation(kotlin("test"))
}

gradlePlugin {
    website.set("https://github.com/StefanOltmann/gradle-msix-plugin")
    vcsUrl.set("https://github.com/StefanOltmann/gradle-msix-plugin")
    plugins {
        create("msix") {
            id = "de.stefan-oltmann.gradle-msix-plugin"
            displayName = "MSIX Packaging"
            description = "Create MSIX resources, manifests, and packages from Gradle."
            tags.set(listOf("msix", "windows", "packaging", "compose"))
            implementationClass = "de.stefan_oltmann.msix.MsixPlugin"
        }
    }
}

kotlin {
    jvmToolchain(17)
}
