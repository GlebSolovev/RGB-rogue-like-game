plugins {
    kotlin("jvm") version "1.6.10"
    kotlin("plugin.serialization") version "1.6.10"
}

group = "ru.hse.sd.rgb"
version = "0.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1")
//    implementation("org.yaml:snakeyaml:1.30")
    implementation("com.charleskorn.kaml:kaml:0.43.0")

    testImplementation(kotlin("test"))
}