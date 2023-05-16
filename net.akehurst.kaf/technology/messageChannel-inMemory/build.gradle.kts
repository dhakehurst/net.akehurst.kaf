
val version_ktor:String by project
val version_coroutines:String by project
val version_kotlinx:String by project

dependencies {
    commonMainImplementation(project(":kaf-common-realisation"))
    commonMainApi(project(":kaf-technology-messageChannel-api"))

    commonMainImplementation("net.akehurst.kotlinx:kotlinx-reflect:$version_kotlinx")
    commonMainImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$version_coroutines") {
        version {
            strictly("$version_coroutines")
        }
    }
    //jvm8MainImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$version_coroutines")
    //jsMainImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:$version_coroutines")
}

exportPublic {
    exportPatterns.set(listOf(
        "net.akehurst.kaf.messageChannel.client.ktor.*"
    ))
}


