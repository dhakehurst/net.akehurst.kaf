
dependencies {

    commonMainApi( project(":kaf-service-logging-api"))
    commonMainApi(libs.nak.kotlinx.collections)

}

exportPublic {
    exportPatterns.set(listOf(
        "net.akehurst.kaf.common.api.*"
    ))
}


