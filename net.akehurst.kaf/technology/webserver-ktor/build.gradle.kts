plugins {
    id("net.akehurst.kotlin.gradle.plugin.jsIntegration")
    id("net.akehurst.kotlinx.kotlinx-reflect-gradle-plugin")
}

dependencies {

    commonMainImplementation(project(":kaf-common-realisation"))
    commonMainImplementation(libs.nak.kotlinx.reflect)

    commonMainApi(project(":kaf-technology-messageChannel-api"))
    commonMainApi(project(":kaf-technology-webserver-api"))

    jvm8MainImplementation(libs.ktor.websockets)
    jvm8MainImplementation(libs.ktor.server.core)
    jvm8MainImplementation(libs.ktor.server.sessions)
    jvm8MainImplementation(libs.ktor.server.websockets)
    jvm8MainImplementation(libs.ktor.server.netty)
    jvm8MainImplementation(libs.ktor.server.default.headers)
    jvm8MainImplementation(libs.ktor.server.call.logging.jvm)

    //jvm8MainImplementation("com.github.lamba92:ktor-spa:$version_ktor_spa")

    commonMainImplementation(libs.kotlinx.coroutines) {
        version {
            strictly(libs.versions.kotlinx.coroutines.get())
        }
    }

    // test
    commonTestImplementation(project(":kaf-service-logging-console"))
    commonTestImplementation(project(":kaf-service-configuration-map"))
    commonTestImplementation(project(":kaf-service-commandLineHandler-simple"))
    commonTestImplementation("ch.qos.logback:logback-classic:+")
}

// define these locations because they are used in multiple places
val ngSrcDir = project.layout.projectDirectory.dir("src/jvm8Test/angular/test-spa")
val ngOutDir = project.layout.buildDirectory.dir("angular")

jsIntegration {
    nodeSrcDirectoryProd.set(ngSrcDir)
    nodeOutDirectoryProd.set(ngOutDir)

    productionCommand.set(mapOf("build" to "ng build --prod --output-path=${ngOutDir.get()}/dist"))
    developmentCommand.set(mapOf("build" to "ng build --output-path=${ngOutDir.get()}/dist"))
}

kotlin {
    sourceSets {
        val jvm8Test by getting {
            resources.srcDir(ngOutDir)
        }
    }
}

kotlinxReflect {
    forReflectionMain.set(listOf(
        "net.akehurst.kaf.engineering.genericMessageChannel"
    ))
}