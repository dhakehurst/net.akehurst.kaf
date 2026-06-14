
dependencies {

    commonMainApi(project(":kaf-service-logging-api"))

    // logging implementation by log4j2
    jvm8MainImplementation(libs.log4j.api)
    jvm8MainRuntimeOnly(libs.log4j.core)

    // Bridge the Log4J 1.2 API
    jvm8MainRuntimeOnly(libs.log4j.bridge.log4j1)
    // Bridge the java.util Logging
    jvm8MainRuntimeOnly(libs.log4j.bridge.jul)
    // Bridge the SLF4J Logging
    jvm8MainRuntimeOnly(libs.log4j.bridge.slf4j)
    // Bridge the Appache Commons Logging
    jvm8MainRuntimeOnly(libs.log4j.bridge.jcl)

}

