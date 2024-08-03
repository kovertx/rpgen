plugins {
    `java-gradle-plugin`
    `embedded-kotlin`
}

dependencies {
    implementation(project(":rpgen-core"))
    api(project(":rpgen-plugin-kotlin"))
    api(project(":rpgen-plugin-typescript"))
    api(project(":rpgen-plugin-openapi"))
}

gradlePlugin {
    plugins {
        create("rpgen-gradle-plugin") {
            id = "io.kovertx.rpgen.rpgen-gradle-plugin"
            implementationClass = "io.kovertx.rpgen.RpGenGradlePlugin"
        }
    }
}
