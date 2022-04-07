plugins {
    kotlin("jvm") version "1.6.10"
}

group = "ru.hse.sd.rgb"
version = "0.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1")
}