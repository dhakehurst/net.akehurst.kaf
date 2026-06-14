
dependencies {

    commonMainImplementation(project(":kaf-service-configuration-api"))
    commonMainImplementation(project(":kaf-common-realisation"))
    commonMainImplementation(project(":kaf-technology-persistence-fs-korio"))
    commonMainImplementation(libs.nak.hjson)
    commonMainImplementation(libs.nak.kserialisation.hjson)
//    commonMainImplementation("com.soywiz.korlibs.klock:klock:$version_klock")

    commonMainImplementation(libs.kotlinx.coroutines) {
        version {
            strictly(libs.versions.kotlinx.coroutines.get())
        }
    }

    commonTestImplementation(project(":kaf-service-logging-console"))
}