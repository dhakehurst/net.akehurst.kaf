val version_komposite: String by project
val version_kxreflect:String by project

val version_neo4j: String by project
val version_neo4j_driver: String by project

dependencies {

    commonMainApi(project(":kaf-api"))
    commonMainApi(project(":kaf-persistence-api"))

    commonMainImplementation(project(":kaf-common"))

    commonMainImplementation("net.akehurst.kotlin.komposite:komposite-common:$version_komposite")
    commonMainImplementation("net.akehurst.kotlinx:kotlinx-collections:$version_kxreflect")
    commonMainImplementation("net.akehurst.kotlinx:kotlinx-reflect:$version_kxreflect")

    jvm8MainImplementation("org.neo4j.driver:neo4j-java-driver:$version_neo4j_driver")
    jvm8MainImplementation("org.neo4j:neo4j:$version_neo4j")


    commonTestImplementation(project(":kaf-service-logging-console"))
    commonTestImplementation(project(":kaf-service-configuration-map"))
    commonTestImplementation(project(":kaf-service-commandLineHandler-simple"))

    // because IntelliJ can't seem to resolve runtime transitive dependencies correctly!!
    commonMainImplementation("net.akehurst.kotlin.komposite:komposite-api:$version_komposite")
    jvm8MainImplementation("net.akehurst.kotlin.komposite:komposite-processor-jvm8:$version_komposite")
}

