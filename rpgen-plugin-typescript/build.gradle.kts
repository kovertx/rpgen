plugins {
    antlr
}

dependencies {
    api(project(":rpgen-core"))
    antlr("org.antlr:antlr4:4.5")
}

tasks.compileKotlin {
    dependsOn(tasks.generateGrammarSource)
}
