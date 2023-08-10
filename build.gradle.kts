import java.io.ByteArrayOutputStream
import java.time.LocalTime

plugins {
    java
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
    }
}

repositories {
    mavenCentral()
    maven("https://www.jitpack.io")
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

configurations.all{
    resolutionStrategy.eachDependency {
        if(this.requested.group == "com.github.Anuken.Arc"){
            this.useVersion("v145.1")
        }
    }
}

dependencies {
    compileOnly("com.github.Anuken.Arc:arc-core:$mindustryVersion")
    compileOnly("com.github.anuken.mindustry:core:$mindustryVersion")
    annotationProcessor("com.github.Anuken:jabel:$jabelVersion")

    testImplementation("com.github.Anuken.Arc:arc-core:$mindustryVersion")
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
            (configurations.compileClasspath.get().toList() + configurations.runtimeClasspath.get().toList() + arrayOf(
                File(platformRoot, "android.jar")
            )).joinToString(" ") { "--classpath ${it.path}" }
//      dex and desugar files - this requires d8 in your PATH
        val err = ByteArrayOutputStream()
        exec {
            commandLine("d8 $dependencies --min-api 14 --output ${archivesName}Android.jar ${archivesName}Desktop.jar")
            workingDir = File("$buildDir/libs")
            errorOutput = err
        }
        println("Errors: $err")
    }
}

tasks.named<Jar>("jar") {
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
    from(zipTree("$buildDir/libs/${archivesName}Desktop.jar"), zipTree("$buildDir/libs/${archivesName}Android.jar"))
    doLast {
        delete("$buildDir/libs/${archivesName}Desktop.jar", "$buildDir/libs/${archivesName}Android.jar")
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}