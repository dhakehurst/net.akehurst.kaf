val version_kotlin: String by project

dependencies {
    commonMainApi(project(":kaf-api"))
    commonMainApi(project(":kaf-service-logging-api"))
    commonMainApi(project(":kaf-service-configuration-api"))
    commonMainApi(project(":kaf-service-commandLineHandler-api"))

    jvm8MainApi(kotlin("reflect"))

    commonTestImplementation(project(":kaf-service-logging-console"))
    commonTestImplementation(project(":kaf-service-configuration-map"))
    commonTestImplementation(project(":kaf-service-commandLineHandler-simple"))
}