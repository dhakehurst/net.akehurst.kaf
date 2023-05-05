
val version_kotlinx:String by project

dependencies {

    commonMainApi( project(":kaf-service-logging-api"))
    commonMainApi( "net.akehurst.kotlinx:kotlinx-collections:$version_kotlinx")

}

exportPublic {
    exportPatterns.set(listOf(
        "net.akehurst.kaf.common.api.*"
    ))
}


