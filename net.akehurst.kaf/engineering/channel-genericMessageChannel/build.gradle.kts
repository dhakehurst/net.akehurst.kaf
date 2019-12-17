
val version_kotlinx: String by project
val version_kserialisation: String by project

dependencies {
    commonMainApi(project(":kaf-technology-messageChannel-api"))

    commonMainImplementation(project(":kaf-common-realisation"))
    commonMainImplementation("net.akehurst.kotlinx:kotlinx-reflect:$version_kotlinx")


    commonTestImplementation(project(":kaf-technology-messageChannel-inMemory"))
    commonTestImplementation("net.akehurst.kotlin.kserialisation:kserialisation-json:${version_kserialisation}")
    commonTestImplementation(project(":kaf-service-logging-console"))
    commonTestImplementation(project(":kaf-service-configuration-map"))
    commonTestImplementation(project(":kaf-service-commandLineHandler-simple"))
}
