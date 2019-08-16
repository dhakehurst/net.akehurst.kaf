package net.akehurst.kaf.technology.persistence.neo4j

import net.akehurst.kaf.api.Application
import net.akehurst.kaf.common.afApplication
import net.akehurst.kaf.service.commandLineHandler.simple.CommandLineHandlerSimple
import net.akehurst.kaf.service.configuration.map.ConfigurationMap
import net.akehurst.kaf.service.logging.api.LogLevel
import net.akehurst.kaf.service.logging.console.LoggingServiceConsole
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class test_PersystentStoreNeo4j : Application {

    companion object {
        val KOMPOSITE = """
            namespace net.akehurst.kaf.technology.persistence.neo4j {
                datatype A {
                  prop { identity(0) }
                }
            }
        """.trimIndent()
    }

    val tempDir = createTempDir(".neo4j_")

    override val af = afApplication(this, "test") {
        defineServices = { commandLineArgs ->
            mapOf(
                    "logging" to LoggingServiceConsole(LogLevel.ALL),
                    "configuration" to ConfigurationMap(mutableMapOf(
                            "sut.embeddedNeo4jDirectory" to tempDir.name
                    )),
                    "cmdLineHandler" to CommandLineHandlerSimple(commandLineArgs)
            )
        }
    }

    val sut = PersistentStoreNeo4j("sut")

    @BeforeTest
    fun startup() {
        this.af.start(listOf())
    }

    @AfterTest
    fun shutdown() {
        this.sut.af.stop()
        this.af.stop()
        tempDir.deleteRecursively()
    }

    @Test
    fun configure() {
        this.sut.configure(mapOf(
                "embedded" to true,
                "uri" to "bolt://localhost:7777",
                "user" to "neo4j",
                "password" to "neo4j",
                "komposite" to KOMPOSITE
        ))
    }

    @Test
    fun create() {
        this.configure()

        val a = A("a")
        sut.create("obj", A::class, a)
    }

}