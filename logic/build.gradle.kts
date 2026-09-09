plugins {
    kotlin("jvm") version "2.2.10"
}

// TODO Dupe from :mod when splitting :mod project into this subproject.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    compileOnly(libs.mindustry)
    compileOnly(libs.arc.core)
    compileOnly(libs.kotlin.stdlib)
}