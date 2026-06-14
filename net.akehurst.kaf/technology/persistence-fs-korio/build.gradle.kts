
dependencies {

    commonMainApi(project(":kaf-technology-persistence-fs-api"))
    commonMainImplementation(project(":kaf-common-realisation"))

    commonMainImplementation(libs.nal.kotlinx.komposite)
    commonMainImplementation(libs.nak.kotlinx.collections)
    commonMainImplementation(libs.nak.kotlinx.reflect)

    commonMainImplementation(libs.korlibs.korio)
    //commonMainImplementation("com.soywiz.korlibs.klock:klock:$version_klock")
    commonMainImplementation(libs.kotlinx.coroutines) {
        version {
            strictly(libs.versions.kotlinx.coroutines.get())
        }
    }

    commonTestImplementation(project(":kaf-service-logging-console"))
    commonTestImplementation(project(":kaf-service-configuration-map"))
    commonTestImplementation(project(":kaf-service-commandLineHandler-simple"))

    // because IntelliJ can't seem to resolve runtime transitive dependencies correctly!!
//    commonMainImplementation("net.akehurst.kotlin.komposite:komposite-api:$version_komposite")
//    commonMainImplementation("net.akehurst.kotlin.komposite:komposite-processor:$version_komposite")
//    commonTestImplementation("net.akehurst.language:agl-processor:$version_agl")
}

