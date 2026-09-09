plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.redwood.schema)
}


redwoodSchema {
    type = "me.mars.triangles.ui.schema.TrianglesUiSchema"
}

dependencies {
    implementation(project(":logic"))

    implementation("com.github.7003mars.maple:schema:0.1.0") // TODO If I set this to compileOnly redwood throws a tantrum. We might need to manually remove this dep from the shadow jar?
    compileOnly(libs.arc.core)
}


tasks.register("codeGen") {
    dependsOn(":schema:widget:redwoodKotlinGenerate", ":schema:compose:redwoodKotlinGenerate")
}

// REMOVEME
subprojects {
    configurations.matching {
        it.name in setOf(
            "jvmRuntimeClasspath",
            "runtimeClasspath",
        )
    }.configureEach {
        exclude(group = "app.cash.redwood")
    }
}

configurations.named("runtimeClasspath") {
    exclude(group = "app.cash.redwood")
    exclude(group = "com.github.7003mars.maple", module = "schema")
}