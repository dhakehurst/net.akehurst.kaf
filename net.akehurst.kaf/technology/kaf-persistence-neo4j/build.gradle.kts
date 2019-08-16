
val version_neo4j: String by project
val version_neo4j_driver: String by project

dependencies {

    commonMainApi(project(":kaf-api"))
    commonMainApi(project(":kaf-persistence-api"))

    commonMainImplementation(project(":kaf-common"))

    jvm8MainImplementation("org.neo4j.driver:neo4j-java-driver:$version_neo4j_driver")
    jvm8MainImplementation("org.neo4j:neo4j:$version_neo4j")


    commonTestImplementation(project(":kaf-service-logging-console"))
    commonTestImplementation(project(":kaf-service-configuration-map"))
    commonTestImplementation(project(":kaf-service-commandLineHandler-simple"))
}

