// Generates the Widget interfaces backends implement

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.redwood.widget)

}

kotlin {
    jvm()
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":logic"))
                compileOnly("me.mars.maple:schema-widget:0.1.0")
                compileOnly(libs.arc.core)
            }
        }
    }
}

redwoodSchema {
    source = project(":schema")
    type = "me.mars.triangles.ui.schema.TrianglesUiSchema"
}