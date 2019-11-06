val version_kotlin: String by project
val version_kotlinx: String by project
val version_coroutines: String by project

dependencies {
    commonMainApi(project(":kaf-common-api"))
    commonMainApi(project(":kaf-service-logging-api"))
    commonMainApi(project(":kaf-service-configuration-api"))
    commonMainApi(project(":kaf-service-commandLineHandler-api"))

    commonMainImplementation("net.akehurst.kotlinx:kotlinx-reflect:$version_kotlinx")
    commonMainImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-common:$version_coroutines")
    jvm8MainImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$version_coroutines")
    jsMainImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:$version_coroutines")

    jvm8MainApi(kotlin("reflect"))

    commonTestImplementation(project(":kaf-service-logging-console"))
    commonTestImplementation(project(":kaf-service-configuration-map"))
    commonTestImplementation(project(":kaf-service-commandLineHandler-simple"))
}