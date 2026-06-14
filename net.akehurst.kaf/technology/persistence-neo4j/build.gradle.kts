plugins {
    alias(libs.plugins.reflect)
}

dependencies {

    commonMainApi(project(":kaf-technology-persistence-api"))
    commonMainImplementation(project(":kaf-common-realisation"))

    commonMainImplementation(libs.nal.agl.processor)
    commonMainImplementation(libs.nal.kotlinx.komposite)
    commonMainImplementation(libs.nak.kotlinx.collections)
    commonMainImplementation(libs.nak.kotlinx.reflect)
    commonMainApi(libs.kotlinx.datetime)
    //commonMainImplementation(libs.korlibs.time)

    jvm8MainImplementation(libs.neo4j.java.driver)
    jvm8MainImplementation(libs.neo4j)


    commonTestImplementation(project(":kaf-service-logging-console"))
    commonTestImplementation(project(":kaf-service-configuration-map"))
    commonTestImplementation(project(":kaf-service-commandLineHandler-simple"))
    //commonTestImplementation("com.soywiz.korlibs.klock:klock:$version_klock")
    //commonMainImplementation("com.soywiz.korlibs.klock:klock:$version_klock") //TODO: remove this when got primitive type mappers

    // because IntelliJ can't seem to resolve runtime transitive dependencies correctly!!
//    commonMainImplementation("net.akehurst.kotlin.komposite:komposite-api:$version_komposite")
//    commonMainImplementation("net.akehurst.kotlin.komposite:komposite-processor:$version_komposite")
//    commonTestImplementation("net.akehurst.language:agl-processor:$version_agl")
//    commonTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$version_coroutines") {
//        version {
//            strictly("$version_coroutines")
//        }
//    }

}

kotlinxReflect {
    forReflectionTest.set(listOf(
        "net.akehurst.kaf.technology.persistence.neo4j.**"
    ))
}
