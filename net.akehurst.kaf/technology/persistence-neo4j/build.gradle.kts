val version_komposite: String by project
val version_kotlinx: String by project

val version_neo4j: String by project
val version_neo4j_driver: String by project
val version_agl: String by project
val version_klock: String by project
val version_coroutines:String by project

dependencies {

    commonMainApi(project(":kaf-technology-persistence-api"))
    commonMainImplementation(project(":kaf-common-realisation"))

    commonMainImplementation("net.akehurst.kotlin.komposite:komposite-common:$version_komposite")
    commonMainImplementation("net.akehurst.kotlinx:kotlinx-collections:$version_kotlinx")
    commonMainImplementation("net.akehurst.kotlinx:kotlinx-reflect:$version_kotlinx")

    jvm8MainImplementation("org.neo4j.driver:neo4j-java-driver:$version_neo4j_driver")
    jvm8MainImplementation("org.neo4j:neo4j:$version_neo4j")


    commonTestImplementation(project(":kaf-service-logging-console"))
    commonTestImplementation(project(":kaf-service-configuration-map"))
    commonTestImplementation(project(":kaf-service-commandLineHandler-simple"))
    commonTestImplementation("com.soywiz.korlibs.klock:klock:$version_klock")
    commonMainImplementation("com.soywiz.korlibs.klock:klock:$version_klock") //TODO: remove this when got primitive type mappers

    // because IntelliJ can't seem to resolve runtime transitive dependencies correctly!!
    commonMainImplementation("net.akehurst.kotlin.komposite:komposite-api:$version_komposite")
    commonMainImplementation("net.akehurst.kotlin.komposite:komposite-processor:$version_komposite")
    commonTestImplementation("net.akehurst.language:agl-processor:$version_agl")
    commonTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$version_coroutines")


}

