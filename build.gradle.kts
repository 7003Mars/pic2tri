import java.io.ByteArrayOutputStream
import java.time.LocalTime

plugins {
    java
    kotlin("jvm") version "1.9.0"
}

version = "1.0"

with(java) {
    targetCompatibility = JavaVersion.VERSION_1_8
    sourceCompatibility = JavaVersion.VERSION_16
}

sourceSets {
    main {
        java {
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
    maven("https://maven.xpdustry.com/anuken")
}
val mindustryVersion by extra("v145.1")
val jabelVersion by extra("93fde537c7")
val sdkRoot: String? by extra(System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT"))

val archivesName = base.archivesName.get()

allprojects {
    tasks.withType<JavaCompile> {
        options.compilerArgs.addAll(arrayOf("--release", "8"))
    }
}

//configurations.all{
//    resolutionStrategy.eachDependency {
//        if(this.requested.group == "com.github.Anuken.Arc"){
//            this.useVersion("v145.1")
//        }
//    }
//}

dependencies {
    compileOnly("com.github.Anuken.Arc:arc-core:$mindustryVersion")
    compileOnly("com.github.anuken.mindustry:core:$mindustryVersion")
    annotationProcessor("com.github.Anuken:jabel:$jabelVersion")

    testImplementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.0")
    testImplementation("com.github.Anuken.Arc:arc-core:$mindustryVersion")
    testImplementation("com.github.Anuken.arc:backend-sdl:$mindustryVersion")
    testRuntimeOnly("com.github.Anuken.arc:natives-desktop:4be3d22cf6")
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.register("jarAndroid") {
    dependsOn("jar")
    doLast{
        if(sdkRoot == null || !File(sdkRoot!!).exists()) throw GradleException("No valid Android SDK found. Ensure that ANDROID_HOME is set to your Android SDK directory.")

        val platformRoot = File("$sdkRoot/platforms/").listFiles()?.also { it.sort(); it.reverse() }?.find { File(it, "android.jar").exists()}

        if(platformRoot == null) throw GradleException("No android.jar found. Ensure that you have an Android platform installed.")

//      collect dependencies needed for desugaring
        val dependencies =
            (configurations.compileClasspath.get().toList() + configurations.runtimeClasspath.get().toList() +
                    arrayOf(File(platformRoot, "android.jar")
            )).joinToString(" ") { "--classpath ${it.path}" }
//      dex and desugar files - this requires d8 in your PATH
        val err = ByteArrayOutputStream()
        val res = exec {
            commandLine("d8 $dependencies --min-api 14 --output ${archivesName}Android.jar ${archivesName}Desktop.jar".split(" "))
            workingDir = File("$buildDir/libs")
            errorOutput = err
            isIgnoreExitValue = true
        }
        logger.warn("Errors: $err")
        res.assertNormalExitValue()
    }
}

tasks.named<Jar>("jar") {
    archiveFileName.set("${archivesName}Desktop.jar")
    val buildVer: String = project.findProperty("modVer") as String? ?: "build-${LocalTime.now()}"

    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it)})
    from("assets/") {
        include("**")
    }
    from("mod.hjson") {
        filter { if (it.startsWith("version")) "version:$buildVer" else it }
    }
}

tasks.register<Jar>("deploy") {
    dependsOn("jar", "jarAndroid")
    archiveFileName.set("$archivesName.jar")
    from(zipTree("$buildDir/libs/${archivesName}Desktop.jar"), zipTree("$buildDir/libs/${archivesName}Android.jar"))
    doLast {
        delete("$buildDir/libs/${archivesName}Desktop.jar", "$buildDir/libs/${archivesName}Android.jar")
    }
}

tasks.named<Test>("test") {
    jvmArgs("-XstartOnFirstThread")
    useJUnitPlatform()
}