plugins {
    kotlin("jvm") version "2.3.0"
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "2.0.0"
}

// Note: Group must have a hyphen to match plugin id
group = "de.stefan-oltmann"
version = "0.1.0"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("org.apache.xmlgraphics:batik-transcoder:1.19")
    implementation("org.apache.xmlgraphics:batik-codec:1.19")
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
