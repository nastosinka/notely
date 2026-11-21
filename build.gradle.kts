import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.compose") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"

    id("org.jetbrains.compose") version "1.7.0"
}

group = "com.notely"
version = "1.0.0"

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material)
    implementation(compose.materialIconsExtended)

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.apache.pdfbox:pdfbox:3.0.8")

    implementation("org.yaml:snakeyaml:2.3")

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

compose.desktop {
    application {
        mainClass = "com.notely.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg)

            packageName = "Notely"
            packageVersion = "1.0.0"
            description = "Personal sheet music library"
        }
    }
}
