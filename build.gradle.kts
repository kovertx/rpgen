import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version embeddedKotlinVersion apply false
    kotlin("plugin.serialization") version embeddedKotlinVersion apply false
    `maven-publish`
}

allprojects {
    group = "io.kovertx.rpgen"
    version = project.findProperty("rpgen.version") as String

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "maven-publish")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

    dependencies {
        implementation(kotlin("stdlib"))
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    }

    publishing {
        publications {
            if (!project.name.endsWith("-gradle-plugin")) {
                register<MavenPublication>("maven") {
                    from(components["java"])
                }
            }
        }
        repositories {
            maven {
                name = "KovertxReleases"
                url = uri("https://mvn.kovertx.io/kovertx-releases")
                credentials {
                    username = "kovertx_deploy"
                    password = System.getenv("MVN_KOVERTX_IO_TOKEN")
                }
            }
        }
    }

    tasks.withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
        }
    }
}

