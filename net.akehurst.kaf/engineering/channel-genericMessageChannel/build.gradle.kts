
val version_kotlinx: String by project

dependencies {
    commonMainApi(project(":kaf-technology-messageChannel-api"))

    commonMainImplementation(project(":kaf-common-realisation"))
    commonMainImplementation("net.akehurst.kotlinx:kotlinx-reflect:$version_kotlinx")
}
