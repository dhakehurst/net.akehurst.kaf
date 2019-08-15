
val version_neo4j_driver: String by project

dependencies {

    commonMainApi(project(":kaf-api"))
    commonMainApi(project(":kaf-persistence-api"))

    jvm8MainImplementation("org.neo4j.driver:neo4j-java-driver:$version_neo4j_driver")
}

