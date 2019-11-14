plugins {
    id("net.akehurst.kotlin.kt2ts-plugin") version "1.0.0"
}

dependencies {

    commonMainApi(project(":kaf-service-api"))

}

val tsdDir ="${buildDir}/tmp/jsJar/ts"

kotlin {
    sourceSets {
        val jsMain by getting {
            resources.srcDir("${tsdDir}")
        }
    }
}

kt2ts {
    localJvmName.set("jvm8")
    modulesConfigurationName.set("jvm8RuntimeClasspath")
    outputDirectory.set(file("${tsdDir}"))
    classPatterns.set(listOf(
            "net.akehurst.kaf.service.logging.api.*"
    ))
}
tasks.getByName("generateTypescriptDefinitionFile").dependsOn("jvm8MainClasses")
tasks.getByName("jsJar").dependsOn("generateTypescriptDefinitionFile")