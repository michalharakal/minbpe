plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
    application
}

group = "sk.ainet.tokenizer"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.8.2")
    implementation("com.github.ajalt.clikt:clikt:5.0.1")
    
    // Add minbpe-kmp library dependency
    // In a real deployment, this would be a published artifact
    // For now, we'll use a local JAR or source dependency
    implementation(project(":minbpe-kmp:library"))
}

application {
    mainClass.set("sk.ainet.tokenizer.kotlin.cli.KotlinCLIKt")
}

kotlin {
    jvmToolchain(21)
}