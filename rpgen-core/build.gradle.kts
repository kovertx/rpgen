plugins {
    antlr
    kotlin("plugin.serialization") version embeddedKotlinVersion
}

dependencies {
    antlr("org.antlr:antlr4:4.5")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.slf4j:slf4j-api:2.0.13")
}

tasks.compileKotlin {
    dependsOn(tasks.generateGrammarSource)
}
