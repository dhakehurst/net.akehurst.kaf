plugins {
    alias(libs.plugins.reflect)
}

dependencies {
    commonMainApi(project(":kaf-technology-messageChannel-api"))

    commonMainImplementation(project(":kaf-common-realisation"))
    commonMainImplementation(libs.nak.kotlinx.reflect)
    commonMainImplementation(libs.nal.kotlinx.komposite)


    commonTestImplementation(project(":kaf-technology-messageChannel-inMemory"))
    commonTestImplementation(libs.nal.agl.processor)
    commonTestImplementation(libs.nak.kserialisation.json)
    commonTestImplementation(project(":kaf-service-logging-console"))
    commonTestImplementation(project(":kaf-service-configuration-map"))
    commonTestImplementation(project(":kaf-service-commandLineHandler-simple"))
}


kotlinxReflect {
    forReflectionTest.set(listOf(
        "net.akehurst.kaf.engineering.channel.genericMessageChannel.**",
        "net.akehurst.kaf.engineering.channel.genericMessageChannel.test.*"
    ))
}

kotlin {
    sourceSets {
        val commonMain by getting {
            kotlin.srcDir("${layout.buildDirectory}/kotlinxReflect/genSrc/commonMain")
        }
    }
}