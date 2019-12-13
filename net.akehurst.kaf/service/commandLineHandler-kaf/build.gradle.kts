dependencies {

    commonMainImplementation(project(":kaf-service-commandLineHandler-api"))

    jvm8TestImplementation(project(":kaf-common-realisation"))
    jvm8TestImplementation(project(":kaf-service-logging-console"))
}