
val version_ktor:String by project

dependencies {
    commonMainImplementation(project(":kaf-common-realisation"))
    commonMainApi(project(":kaf-technology-messageChannel-api"))

    commonMainImplementation("io.ktor:ktor-client-websockets:$version_ktor")
    jvm8MainImplementation("io.ktor:ktor-client-websockets-jvm:$version_ktor")
    jsMainImplementation("io.ktor:ktor-client-websockets-js:$version_ktor")

}

kotlin {
    sourceSets {
        //need to add this because kotlinx.io has a dependency on it
        //webpack fails to find 'text-encoding'
        val jsTest by getting {
            dependencies {
                api(npm("text-encoding","0.7.0"))
            }
        }
    }
}
/*
kt2ts {
    jvmTargetName.set("jvm8")
    classPatterns.set(listOf(
            "net.akehurst.kaf.technology.messageChannel.websocket.client.ktor.*"
    ))
}

 */
