package net.akehurst.kaf.technology.persistence.neo4j

import net.akehurst.kaf.api.Application
import net.akehurst.kaf.common.afApplication
import net.akehurst.kaf.service.commandLineHandler.simple.CommandLineHandlerSimple
import net.akehurst.kaf.service.configuration.map.ConfigurationMap
import net.akehurst.kaf.service.logging.api.LogLevel
import net.akehurst.kaf.service.logging.console.LoggingServiceConsole
import net.akehurst.kaf.technology.persistence.api.FilterProperty
import java.nio.file.Files
import kotlin.test.*

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
                            "sut.embeddedNeo4jDirectory" to tempDir.absoluteFile.toString()
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
        while (tempDir.exists()) {
            println("deleting ${tempDir.absoluteFile}")
            tempDir.deleteRecursively()
        }
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
        sut.create(A::class, a)
    }

    @Test
    fun createAll() {
        this.configure()

        val a1 = A("a1")
        val a2 = A("a2")
        val a3 = A("a3")
        val a4 = A("a4")
        val setOfA = setOf(a1, a2, a3, a4)
        sut.createAll(A::class, setOfA)
    }

    @Test
    fun read() {
        // given
        this.configure()
        val a = A("a")
        sut.create(A::class, a)

        // when
        val filter = FilterProperty("prop", "a")
        val actual = sut.read(A::class, setOf(filter))

        // then
        val expected = a
        assertNotNull(actual)
        assertEquals(expected, actual)
    }
}