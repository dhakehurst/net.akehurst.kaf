val version_ktor: String by project
val version_coroutines: String by project
val version_ktor_spa: String = "1.1.4"

//repositories {
//    maven {
//        setUrl("https://jitpack.io")
//    }
//}

dependencies {

    commonMainImplementation(project(":kaf-common-realisation"))

    commonMainApi(project(":kaf-technology-messageChannel-api"))
    commonMainApi(project(":kaf-technology-webserver-api"))

    jvm8MainImplementation("io.ktor:ktor-websockets:$version_ktor")
    jvm8MainImplementation("io.ktor:ktor-server-core:$version_ktor")
    jvm8MainImplementation("io.ktor:ktor-server-netty:$version_ktor")
    //jvm8MainImplementation("com.github.lamba92:ktor-spa:$version_ktor_spa")

    commonMainImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-native") {
        version {
            strictly("$version_coroutines")
        }
    }

    // test
    commonTestImplementation(project(":kaf-service-logging-console"))
    commonTestImplementation(project(":kaf-service-configuration-map"))
    commonTestImplementation(project(":kaf-service-commandLineHandler-simple"))
}