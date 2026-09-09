plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.redwood.schema) apply false
    alias(libs.plugins.redwood.widget) apply false
    alias(libs.plugins.redwood.modifiers) apply false
    alias(libs.plugins.redwood.compose) apply false
}


subprojects {
    group = "me.mars.triangles"

    plugins.withType<JavaPlugin> {
        configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(17))
            }
        }
    }

    repositories {
        mavenCentral()
        mavenLocal()
        google()
        maven("https://maven.xpdustry.com/mindustry")
        maven("https://www.jitpack.io")
    }
}