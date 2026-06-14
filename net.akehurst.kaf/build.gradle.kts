/**
 * Copyright (C) 2019 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import com.github.gmazzo.buildconfig.BuildConfigExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile

plugins {
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.buildconfig) apply false
    alias(libs.plugins.credentials) apply true
    alias(libs.plugins.exportPublic) apply false
    alias(libs.plugins.reflect) apply false
    alias(libs.plugins.jsIntegration) apply false
}
val kotlin_languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0
val kotlin_apiVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0
val jvmTargetVersion = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21

allprojects {
    group = rootProject.name
    version = rootProject.libs.versions.project.get()

    project.layout.buildDirectory = File(rootProject.projectDir, ".gradle-build/${project.name}")
}

subprojects {

    apply(plugin = "org.jetbrains.kotlin.multiplatform")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "com.github.gmazzo.buildconfig")
    apply(plugin = "net.akehurst.kotlin.gradle.plugin.exportPublic")

    repositories {
        mavenLocal {
            content {
                includeGroupByRegex("net\\.akehurst.+")
            }
        }
        mavenCentral()
    }

    configure<BuildConfigExtension> {
        val now = java.time.Instant.now()
        fun fBbuildStamp(): String = java.time.format.DateTimeFormatter.ISO_DATE_TIME.withZone(java.time.ZoneId.of("UTC")).format(now)
        fun fBuildDate(): String = java.time.format.DateTimeFormatter.ofPattern("yyyy-MMM-dd").withZone(java.time.ZoneId.of("UTC")).format(now)
        fun fBuildTime(): String = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss z").withZone(java.time.ZoneId.of("UTC")).format(now)

        packageName("${project.group}.${project.name.substringAfter("kaf-").replace("-", ".")}")
        buildConfigField("String", "version", "\"${project.version}\"")
        buildConfigField("String", "buildStamp", "\"${fBbuildStamp()}\"")
        buildConfigField("String", "buildDate", "\"${fBuildDate()}\"")
        buildConfigField("String", "buildTime", "\"${fBuildTime()}\"")
    }

    configure<KotlinMultiplatformExtension> {
        jvm("jvm8") {
            val main by compilations.getting {
                compileTaskProvider.configure {
                    compilerOptions {
                        languageVersion.set(kotlin_languageVersion)
                        apiVersion.set(kotlin_apiVersion)
                        jvmTarget.set(jvmTargetVersion)
                    }
                }
            }
            val test by compilations.getting {
                compileTaskProvider.configure {
                    compilerOptions {
                        languageVersion.set(kotlin_languageVersion)
                        apiVersion.set(kotlin_apiVersion)
                        jvmTarget.set(jvmTargetVersion)
                    }
                }
            }
        }
        js("js",IR) {
            binaries.library()
            generateTypeScriptDefinitions()
            compilerOptions {
                target.set("es2015")
            }
            nodejs {
                testTask {
                    useMocha {
                        timeout = "5000"
                    }
                }
            }
            browser {
                webpackTask {
                    outputFileName = "${project.group}-${project.name}.js"
                }
                testTask {
                    useMocha {
                        timeout = "5000"
                    }
                }
            }
        }

        sourceSets {
            all {
                languageSettings.optIn("kotlin.ExperimentalStdlibApi")
            }
        }
    }

    //val dokkaHtml by tasks.getting(org.jetbrains.dokka.gradle.DokkaTask::class)

    val javadocJar: TaskProvider<Jar> by tasks.registering(Jar::class) {
        //dependsOn(dokkaHtml)
        archiveClassifier.set("javadoc")
        //from(dokkaHtml.outputDirectory)
    }
    tasks.named("publish").get().dependsOn("javadocJar")

    dependencies {
        "commonTestImplementation"(kotlin("test"))
        "commonTestImplementation"(kotlin("test-annotations-common"))
    }

    fun getProjectProperty(s: String) = project.findProperty(s) as String?

    val creds = project.properties["credentials"] as nu.studer.gradle.credentials.domain.CredentialsContainer
    val sonatype_pwd = creds.forKey("SONATYPE_PASSWORD") as String?
        ?: getProjectProperty("SONATYPE_PASSWORD")
        ?: error("Must set project property with Sonatype Password (-P SONATYPE_PASSWORD=<...> or set in ~/.gradle/gradle.properties)")
    project.ext.set("signing.password", sonatype_pwd)

}