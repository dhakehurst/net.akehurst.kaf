
val version_log4j: String by project

dependencies {

    commonMainApi(project(":kaf-service-logging-api"))

    // logging implementation by log4j2
    jvm8MainImplementation("org.apache.logging.log4j:log4j-api:$version_log4j")
    jvm8MainRuntimeOnly("org.apache.logging.log4j:log4j-core:$version_log4j")

    // Bridge the Log4J 1.2 API
    jvm8MainRuntimeOnly("org.apache.logging.log4j:log4j-1.2-api:$version_log4j")
    // Bridge the java.util Logging
    jvm8MainRuntimeOnly("org.apache.logging.log4j:log4j-jul:$version_log4j")
    // Bridge the SLF4J Logging
    jvm8MainRuntimeOnly("org.apache.logging.log4j:log4j-slf4j-impl:$version_log4j")
    // Bridge the Appache Commons Logging
    jvm8MainRuntimeOnly("org.apache.logging.log4j:log4j-jcl:$version_log4j")

}

