plugins {
    antlr
}

dependencies {
    api(project(":rpgen-core"))
    antlr("org.antlr:antlr4:4.5")
    implementation("io.github.reidsync:kotlin-json-patch:1.0.0")
}

tasks.compileKotlin {
    dependsOn(tasks.generateGrammarSource)
}
