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
    implementation("org.yaml:snakeyaml:2.3")
    implementation("org.apache.pdfbox:pdfbox:3.0.8")

    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}


kotlin {
    jvmToolchain(21)
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
