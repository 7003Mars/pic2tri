import org.gradle.kotlin.dsl.support.serviceOf
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.time.LocalTime

plugins {
    java
    kotlin("jvm") version "2.2.10"
    id("com.gradleup.shadow") version "9.0.2"
    id("xyz.wagyourtail.jvmdowngrader") version "1.3.5"
}

version = "1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(16))
    }
}

sourceSets {
    main {
        java {
            srcDirs("src")
        }
        kotlin {
            srcDirs("src")
        }
    }
    test {
        java {
            srcDirs("test")
        }
        kotlin {
            srcDirs("test")
        }
    }
}

repositories {
    mavenCentral()
    maven("https://www.jitpack.io")
    maven("https://raw.githubusercontent.com/Zelaux/MindustryRepo/master/repository")
    maven("https://maven.xpdustry.com/anuken")
}
val mindustryVersion by extra("v154.3")
val sdkRoot: String? by extra(System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT"))

val archivesName: String = base.archivesName.get()


//configurations.all{
//    resolutionStrategy.eachDependency {
//        if(this.requested.group == "com.github.Anuken.Arc"){
//            this.useVersion("v146")
//        }
//    }
//}

dependencies {
    compileOnly("com.github.Anuken.Arc:arc-core:$mindustryVersion")
    compileOnly("com.github.anuken.mindustry:core:$mindustryVersion")
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    // TODO DELETEME Above
//    testImplementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.0")
    testImplementation("com.github.anuken.mindustry:core:${mindustryVersion}")
    testImplementation("com.github.Anuken.Arc:arc-core:$mindustryVersion")
    testImplementation("com.github.Anuken.Arc:backend-sdl:$mindustryVersion")
    testImplementation("com.github.Anuken.Arc:backend-headless:${mindustryVersion}")
    testRuntimeOnly("com.github.Anuken.Arc:natives-desktop:$mindustryVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.register<Exec>("jarAndroid") {
    dependsOn("shadeDowngradedApi")
    if(sdkRoot == null || !File(sdkRoot!!).exists()) throw GradleException("No valid Android SDK found. Ensure that ANDROID_HOME is set to your Android SDK directory.")

    val platformRoot = File("$sdkRoot/platforms/").listFiles()?.also { it.sort(); it.reverse() }?.find { File(it, "android.jar").exists()}
    if(platformRoot == null) throw GradleException("No android.jar found. Ensure that you have an Android platform installed.")
    val d8Path = System.getenv("d8_path") ?: "d8"
    // collect dependencies needed for desugaring
    val dependencies = (
            configurations.compileClasspath.get().toList() + configurations.runtimeClasspath.get().toList()
            ).joinToString(" ") { "--classpath ${it.path}" }
    val libPath = "--lib ${File(platformRoot, "android.jar").path}"
    workingDir(layout.buildDirectory.dir("libs"))

    commandLine("$d8Path $libPath $dependencies --min-api 14 --output ${archivesName}Android.jar ${archivesName}Desktop.jar".split(" "))
}


// We don't actually need the verbose way of obtaining the task as they both do the same thing
tasks.shadowJar {

    val buildVer: String = project.findProperty("modVer") as String? ?: "build-${LocalTime.now()}"

    from("assets/") {
        include("**")
    }
    from("mod.hjson") {
        filter { if (it.startsWith("version")) "version:$buildVer" else it }
    }

    minimize()
    enableAutoRelocation = true
}

tasks.shadeDowngradedApi {
    archiveFileName.set("${archivesName}Desktop.jar")
}

tasks.register<Jar>("deploy") {
    dependsOn("shadeDowngradedApi", "jarAndroid")
    archiveFileName.set("$archivesName.jar")
    val name = archivesName
    val buildDir = layout.buildDirectory
    from(
        buildDir.file("libs/${name}Desktop.jar").map { zipTree(it) },
        buildDir.file("libs/${name}Android.jar").map { zipTree(it) }
    )
    val fs = project.serviceOf<FileSystemOperations>()
    doLast {
        fs.delete {
            delete(buildDir.file("libs/${name}Desktop.jar"), buildDir.file("libs/${name}Android.jar"))
        }
    }
}

tasks.named<Test>("test") {
    jvmArgs("-XstartOnFirstThread")
    useJUnitPlatform()
}