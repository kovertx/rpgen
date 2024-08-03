plugins {
    application
}

dependencies {
    implementation("com.github.ajalt.clikt:clikt:4.2.2")

    implementation(project(":rpgen-core"))
    implementation(project(":rpgen-plugin-kotlin"))
    implementation(project(":rpgen-plugin-openapi"))
    implementation(project(":rpgen-plugin-typescript"))
}

application {
    mainClass = "io.kovertx.rpgen.app.RpGenAppKt"
    applicationName = "rpgen"
}
