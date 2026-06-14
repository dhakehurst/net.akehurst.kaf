import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNpmResolutionManager

dependencies {

    commonMainApi(project(":kaf-technology-persistence-api"))
    commonMainApi(project(":kaf-technology-persistence-fs-api"))
    commonMainImplementation(project(":kaf-common-realisation"))

    commonMainApi(libs.nak.kserialisation.hjson)
    //commonMainImplementation(libs.korlibs.time) //TODO: remove this when expose primitive type mappers
    commonMainImplementation(libs.nal.agl.processor)
    commonMainImplementation(libs.nal.kotlinx.komposite)
    // commonMainImplementation("net.akehurst.kotlinx:kotlinx-collections:$version_kotlinx")
    // commonMainImplementation("net.akehurst.kotlinx:kotlinx-reflect:$version_kotlinx")

    commonTestImplementation(project(":kaf-service-logging-console"))
    commonTestImplementation(project(":kaf-service-configuration-map"))
    commonTestImplementation(project(":kaf-service-commandLineHandler-simple"))
    //commonTestImplementation("com.soywiz.korlibs.klock:klock:$version_klock")
    commonTestImplementation(libs.korlibs.korio)


    // because IntelliJ can't seem to resolve runtime transitive dependencies correctly!!
//    commonTestImplementation("net.akehurst.kotlinx:kotlinx-collections:$version_kotlinx")
//    commonTestImplementation("net.akehurst.kotlinx:kotlinx-reflect:$version_kotlinx")
//    commonTestImplementation("net.akehurst.kotlin.hjson:hjson:$version_hjson")
//    commonTestImplementation("net.akehurst.kotlin.komposite:komposite-api:$version_komposite")
//    commonTestImplementation("net.akehurst.kotlin.komposite:komposite-processor:$version_komposite")
//    commonTestImplementation("net.akehurst.language:agl-processor:$version_agl")
//    commonTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$version_coroutines") {
//        version {
//            strictly("$version_coroutines")
//        }
//    }
}

