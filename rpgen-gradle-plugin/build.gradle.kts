plugins {
    `java-gradle-plugin`
    `embedded-kotlin`
}

dependencies {
    implementation(project(":rpgen-core"))
}

gradlePlugin {
    plugins {
        create("rpgen-gradle-plugin") {
            id = "io.kovertx.rpgen.rpgen-gradle-plugin"
            implementationClass = "io.kovertx.rpgen.RpGenGradlePlugin"
        }
    }
}
