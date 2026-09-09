plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    // TODO This was slopped, find a proper fix?
    // Comments by chatgpt:  api: TrianglesUiWidgetSystem() returns TrianglesUiSchemaWidgetSystem, so consumers need this on their compile classpath
    api(project(":schema:widget"))
    implementation(project(":logic"))

    compileOnly("me.mars.maple:renderer:0.1.0")
    compileOnly("me.mars.maple:schema-widget:0.1.0") // TODO We need this as we need the ArcUiWidgetFactory class extends me.mars.maple.schema.widget.PrimitivesWidgetFactory.
    // Perhaps its not included as a transitive dep as we set the renderer dep as compileOnly?


    compileOnly(libs.arc.core)
    compileOnly(libs.mindustry)

//    implementation(libs.redwood.widget)
}