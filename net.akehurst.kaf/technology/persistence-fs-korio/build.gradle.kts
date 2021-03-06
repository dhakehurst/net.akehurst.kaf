val version_komposite: String by project
val version_kotlinx:String by project

val version_korio: String by project
val version_agl: String by project
val version_klock: String by project
val version_coroutines: String by project

dependencies {

    commonMainApi(project(":kaf-technology-persistence-fs-api"))
    commonMainImplementation(project(":kaf-common-realisation"))

    commonMainImplementation("net.akehurst.kotlin.komposite:komposite-common:$version_komposite")
    commonMainImplementation("net.akehurst.kotlinx:kotlinx-collections:$version_kotlinx")
    commonMainImplementation("net.akehurst.kotlinx:kotlinx-reflect:$version_kotlinx")

    commonMainImplementation("com.soywiz.korlibs.korio:korio:$version_korio")
    commonMainImplementation("com.soywiz.korlibs.klock:klock:$version_klock") {
        isForce = true
    }
    commonMainImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-common:$version_coroutines") {
        isForce=true
    }
    jvm8MainImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$version_coroutines") {
        isForce=true
    }
    jsMainImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:$version_coroutines") {
        isForce=true
    }

    commonTestImplementation(project(":kaf-service-logging-console"))
    commonTestImplementation(project(":kaf-service-configuration-map"))
    commonTestImplementation(project(":kaf-service-commandLineHandler-simple"))

    // because IntelliJ can't seem to resolve runtime transitive dependencies correctly!!
    commonMainImplementation("net.akehurst.kotlin.komposite:komposite-api:$version_komposite")
    commonMainImplementation("net.akehurst.kotlin.komposite:komposite-processor:$version_komposite")
    commonTestImplementation("net.akehurst.language:agl-processor:$version_agl")
}

