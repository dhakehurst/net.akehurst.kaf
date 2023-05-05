
dependencies {

    commonMainApi(project(":kaf-service-api"))

}

exportPublic {
    exportPatterns.set(listOf(
        "net.akehurst.kaf.service.logging.api.*"
    ))
}

