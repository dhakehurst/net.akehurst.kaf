dependencies {

    commonMainImplementation(project(":kaf-service-commandLineHandler-api"))
    jvm8MainApi("com.github.ajalt:clikt:1.7.0")


    jvm8TestImplementation(project(":kaf-common-realisation"))
    jvm8TestImplementation(project(":kaf-service-logging-console"))
}