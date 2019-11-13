plugins {
    id("net.akehurst.kotlin.kt2ts-plugin") version "1.0.0"
}

val version_kotlinx:String by project

dependencies {

    commonMainApi( project(":kaf-service-logging-api"))
    commonMainApi( "net.akehurst.kotlinx:kotlinx-collections:$version_kotlinx")

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
    packageJsonDir.set(file("${tsdDir}"))
    declarationsFile.set(file("${tsdDir}/${project.group}-${project.name}.d.ts"))
    classPatterns.set(listOf(
            "net.akehurst.kaf.common.api.Identifiable"
    ))
}
tasks.getByName("generateTypescriptDefinitionFile").dependsOn("jvm8MainClasses")
tasks.getByName("jsJar").dependsOn("generateTypescriptDefinitionFile")