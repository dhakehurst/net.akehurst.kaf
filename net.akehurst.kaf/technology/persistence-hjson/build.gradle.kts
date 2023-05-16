val version_komposite: String by project
val version_kserialisation: String by project
val version_kotlinx: String by project

val version_korio:String by project

val version_agl: String by project
val version_hjson: String by project
val version_klock: String by project
val version_coroutines: String by project

dependencies {

    commonMainApi(project(":kaf-technology-persistence-api"))
    commonMainApi(project(":kaf-technology-persistence-fs-api"))
    commonMainImplementation(project(":kaf-common-realisation"))

    commonMainImplementation("net.akehurst.kotlin.kserialisation:kserialisation-hjson:$version_kserialisation")
    commonMainImplementation("com.soywiz.korlibs.klock:klock:$version_klock") //TODO: remove this when expose primitive type mappers
    commonMainImplementation("net.akehurst.kotlin.komposite:komposite-common:$version_komposite")
    // commonMainImplementation("net.akehurst.kotlinx:kotlinx-collections:$version_kotlinx")
    // commonMainImplementation("net.akehurst.kotlinx:kotlinx-reflect:$version_kotlinx")

    commonTestImplementation(project(":kaf-service-logging-console"))
    commonTestImplementation(project(":kaf-service-configuration-map"))
    commonTestImplementation(project(":kaf-service-commandLineHandler-simple"))
    commonTestImplementation("com.soywiz.korlibs.klock:klock:$version_klock")
    commonTestImplementation("com.soywiz.korlibs.korio:korio:$version_korio")


    // because IntelliJ can't seem to resolve runtime transitive dependencies correctly!!
    commonTestImplementation("net.akehurst.kotlinx:kotlinx-collections:$version_kotlinx")
    commonTestImplementation("net.akehurst.kotlinx:kotlinx-reflect:$version_kotlinx")
    commonTestImplementation("net.akehurst.kotlin.hjson:hjson:$version_hjson")
    commonTestImplementation("net.akehurst.kotlin.komposite:komposite-api:$version_komposite")
    commonTestImplementation("net.akehurst.kotlin.komposite:komposite-processor:$version_komposite")
    commonTestImplementation("net.akehurst.language:agl-processor:$version_agl")
    commonTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$version_coroutines") {
        version {
            strictly("$version_coroutines")
        }
    }
}

