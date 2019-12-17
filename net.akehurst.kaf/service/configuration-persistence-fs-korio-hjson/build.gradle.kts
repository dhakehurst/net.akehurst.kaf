val version_klock:String by project
val version_coroutines:String by project

val version_korio:String by project
val version_agl: String by project
val version_hjson: String by project
val version_komposite: String by project
val version_kserialisation: String by project
val version_kotlinx: String by project

dependencies {

    commonMainImplementation(project(":kaf-service-configuration-api"))
    commonMainImplementation(project(":kaf-common-realisation"))
    commonMainImplementation("net.akehurst.kotlin.hjson:hjson:$version_hjson")
    commonMainImplementation("net.akehurst.kotlin.kserialisation:kserialisation-hjson:$version_kserialisation")
    commonMainImplementation(project(":kaf-technology-persistence-fs-korio"))


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

    // because IntelliJ can't seem to resolve runtime transitive dependencies correctly!!
    /*
    commonTestImplementation("com.soywiz.korlibs.korio:korio:$version_korio")
    commonTestImplementation("net.akehurst.kotlinx:kotlinx-collections:$version_kotlinx")
    commonTestImplementation("net.akehurst.kotlinx:kotlinx-reflect:$version_kotlinx")
    commonTestImplementation("net.akehurst.kotlin.hjson:hjson:$version_hjson")
    commonTestImplementation("net.akehurst.kotlin.komposite:komposite-api:$version_komposite")
    commonTestImplementation("net.akehurst.kotlin.komposite:komposite-common:$version_komposite")
    commonTestImplementation("net.akehurst.kotlin.komposite:komposite-processor:$version_komposite")
    commonTestImplementation("net.akehurst.language:agl-processor:$version_agl")
    commonTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-common:$version_coroutines")
    jvm8TestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$version_coroutines")
    jsTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:$version_coroutines")
     */
}