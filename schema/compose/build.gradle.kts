// Generates the Composables that users will use.

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.redwood.compose)
}

kotlin {
    jvm()
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":schema:widget"))
                implementation(project(":logic"))

//                compileOnly("me.mars.maple:schema-compose:0.1.0")
                compileOnly(libs.arc.core)
            }
        }
    }
}

redwoodSchema {
    source = project(":schema")
    type = "me.mars.triangles.ui.schema.TrianglesUiSchema"
}