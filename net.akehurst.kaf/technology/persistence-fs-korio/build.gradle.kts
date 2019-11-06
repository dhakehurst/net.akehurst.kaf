val version_komposite: String by project
val version_kotlinx:String by project

val version_korio = "1.8.4"
val version_agl: String by project
val version_klock: String by project

dependencies {

    commonMainApi(project(":kaf-technology-persistence-fs-api"))
    commonMainImplementation(project(":kaf-common-realisation"))

    commonMainImplementation("net.akehurst.kotlin.komposite:komposite-common:$version_komposite")
    commonMainImplementation("net.akehurst.kotlinx:kotlinx-collections:$version_kotlinx")
    commonMainImplementation("net.akehurst.kotlinx:kotlinx-reflect:$version_kotlinx")

    commonMainImplementation("com.soywiz.korlibs.korio:korio:$version_korio")


    commonTestImplementation(project(":kaf-service-logging-console"))
    commonTestImplementation(project(":kaf-service-configuration-map"))
    commonTestImplementation(project(":kaf-service-commandLineHandler-simple"))

    // because IntelliJ can't seem to resolve runtime transitive dependencies correctly!!
    commonMainImplementation("net.akehurst.kotlin.komposite:komposite-api:$version_komposite")
    commonMainImplementation("net.akehurst.kotlin.komposite:komposite-processor:$version_komposite")
    commonTestImplementation("net.akehurst.language:agl-processor:$version_agl")
}

