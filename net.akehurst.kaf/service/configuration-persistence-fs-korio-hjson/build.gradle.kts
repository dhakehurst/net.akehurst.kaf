val version_korio:String by project
val version_klock:String by project
val version_coroutines:String by project

dependencies {

    commonMainImplementation(project(":kaf-service-configuration-api"))

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
}