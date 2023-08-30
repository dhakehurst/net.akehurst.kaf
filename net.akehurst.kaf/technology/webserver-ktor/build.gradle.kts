plugins {
    id("net.akehurst.kotlin.gradle.plugin.jsIntegration")
    id("net.akehurst.kotlinx.kotlinx-reflect-gradle-plugin")
}

val version_ktor: String by project
val version_coroutines: String by project
val version_kotlinx: String by project
//val version_ktor_spa: String = "1.1.4"

//repositories {
//    maven {
//        setUrl("https://jitpack.io")
//    }
//}

dependencies {

    commonMainImplementation(project(":kaf-common-realisation"))
    commonMainImplementation("net.akehurst.kotlinx:kotlinx-reflect:$version_kotlinx")

    commonMainApi(project(":kaf-technology-messageChannel-api"))
    commonMainApi(project(":kaf-technology-webserver-api"))

    jvm8MainImplementation("io.ktor:ktor-websockets:$version_ktor")
    jvm8MainImplementation("io.ktor:ktor-server-core:$version_ktor")
    jvm8MainImplementation("io.ktor:ktor-server-sessions:$version_ktor")
    jvm8MainImplementation("io.ktor:ktor-server-websockets:$version_ktor")
    jvm8MainImplementation("io.ktor:ktor-server-netty:$version_ktor")
    jvm8MainImplementation("io.ktor:ktor-server-default-headers:$version_ktor")
    jvm8MainImplementation("io.ktor:ktor-server-call-logging-jvm:$version_ktor")
    //jvm8MainImplementation("com.github.lamba92:ktor-spa:$version_ktor_spa")

    commonMainImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core") {
        version {
            strictly("$version_coroutines")
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
    nodeSrcDirectory.set(ngSrcDir)
    nodeOutDirectory.set(ngOutDir)

    productionCommand.set("ng build --prod --output-path=${ngOutDir.get()}/dist")
    developmentCommand.set("ng build --output-path=${ngOutDir.get()}/dist")
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