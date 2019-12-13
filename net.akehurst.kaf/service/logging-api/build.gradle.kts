
dependencies {

    commonMainApi(project(":kaf-service-api"))

}


kt2ts {
    jvmTargetName.set("jvm8")
    classPatterns.set(listOf(
            "net.akehurst.kaf.service.logging.api.*"
    ))
}
