import java.time.LocalTime

plugins {
    java
}

version = "1.0"

with(java) {
    targetCompatibility = JavaVersion.VERSION_1_8
    sourceCompatibility = JavaVersion.VERSION_16

}

sourceSets.main.configure {
    java.srcDirs("src")
}

repositories {
    mavenCentral()
    maven("https://www.jitpack.io")
}
val mindustryVersion by extra("v144.3")
val jabelVersion by extra("93fde537c7")
val sdkRoot: String? by extra(System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT"))

allprojects {
    tasks.withType<JavaCompile> {
        options.compilerArgs.addAll(arrayOf("--release", "8"))
    }
}

dependencies {
    compileOnly("com.github.Anuken.Arc:arc-core:$mindustryVersion")
    compileOnly("com.github.Anuken.mindustryjitpack:core:$mindustryVersion")
    annotationProcessor("com.github.Anuken:jabel:$jabelVersion")
}

tasks.register("jarAndroid") {
    dependsOn("jar")
    doLast{

        if(sdkRoot == null || !File(sdkRoot!!).exists()) throw GradleException("No valid Android SDK found. Ensure that ANDROID_HOME is set to your Android SDK directory.")

        val platformRoot = File("$sdkRoot/platforms/").listFiles()?.also { it.sort(); it.reverse() }?.find { File(it, "android.jar").exists()}

        if(platformRoot == null) throw GradleException("No android.jar found. Ensure that you have an Android platform installed.")

//      collect dependencies needed for desugaring
        val dependencies = configurations.compileClasspath.get().toList() + configurations.runtimeClasspath.get().toList() + arrayOf(File(platformRoot, "android.jar")).map{ "--classpath $it.path" }.joinToString(" ")
//      dex and desugar files - this requires d8 in your PATH
        exec {
            commandLine("d8 $dependencies --min-api 14 --output ${base.archivesName}Android.jar ${base.archivesName}Desktop.jar")
            workingDir = File("$buildDir/libs")
        }
    }
}

tasks.named<Jar>("jar") {
//    val archiveFileName = "${base.archivesName}Desktop.jar"
    val buildVer: String = project.findProperty("modVer") as String? ?: "build-${LocalTime.now()}"

    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it)})
    from("assets/") {
        include("**")
    }
    from("mod.hjson") {
        filter { if (it.startsWith("version")) "version:$buildVer" else it }
    }
}

tasks.named<Jar>("deploy") {
    dependsOn("jar", "jarAndroid")
    from(zipTree("$buildDir/libs/${base.archivesName}Desktop.jar"), zipTree("$buildDir/libs/${base.archivesName}Android.jar"))
    doLast {
        delete("$buildDir/libs/${base.archivesName}Desktop.jar", "$buildDir/libs/${base.archivesName}Android.jar")
    }
}