val version_transform: String by project

dependencies {
    commonMainApi(project(":kaf-persistence-api"))

    commonMainImplementation("net.akehurst.transform:transform-binary:$version_transform")

    jvm8MainImplementation("org.jetbrains.exposed:exposed:0.14.1")

    jvm8TestRuntime("com.h2database:h2:1.4.197")
    jvm8TestRuntime("org.postgresql:postgresql:42.0.0")

}

