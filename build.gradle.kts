plugins {
    id("java-library")
}

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.17-R0.1-SNAPSHOT")
    implementation("org.xerial:sqlite-jdbc:3.53.0.0")
    implementation("com.google.code.gson:gson:2.14.0")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(16)
}

tasks {
    processResources {
        val props = mapOf("version" to version, "description" to project.description)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
