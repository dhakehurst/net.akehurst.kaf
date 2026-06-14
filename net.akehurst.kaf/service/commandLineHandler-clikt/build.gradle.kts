dependencies {

    commonMainImplementation(project(":kaf-service-commandLineHandler-api"))
    commonMainImplementation(libs.clikt)

    jvm8TestImplementation(project(":kaf-common-realisation"))
    jvm8TestImplementation(project(":kaf-service-logging-console"))
}