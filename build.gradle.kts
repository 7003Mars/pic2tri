plugins {
    id("java")
}

sourceSets {
    main {
        java {
            srcDirs("src")
        }
    }
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.anuken.arc:arc-core:v143.1")

}


tasks.jar {
    manifest {
       attributes["Main-Class"] = "me.mars.triangles.Goof"
    }
    from({
        configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }
    })
}