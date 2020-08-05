
val version_kotlinx:String by project

dependencies {

    commonMainApi( project(":kaf-service-logging-api"))
    commonMainApi( "net.akehurst.kotlinx:kotlinx-collections:$version_kotlinx")

}


kt2ts {
    jvmTargetName.set("jvm8")
    classPatterns.set(listOf(
            "net.akehurst.kaf.common.api.*"
    ))
}

