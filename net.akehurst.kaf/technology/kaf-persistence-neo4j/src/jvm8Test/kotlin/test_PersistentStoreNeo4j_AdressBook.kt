package net.akehurst.kaf.technology.persistence.neo4j

import com.soywiz.klock.DateTime
import com.soywiz.klock.Month
import com.soywiz.klock.Year
import net.akehurst.kaf.api.Application
import net.akehurst.kaf.common.afApplication
import net.akehurst.kaf.service.commandLineHandler.simple.CommandLineHandlerSimple
import net.akehurst.kaf.service.configuration.map.ConfigurationMap
import net.akehurst.kaf.service.logging.api.LogLevel
import net.akehurst.kaf.service.logging.console.LoggingServiceConsole
import net.akehurst.kaf.technology.persistence.api.FilterProperty
import kotlin.test.*

class test_PersystentStoreNeo4j_AddressBook : Application {

    companion object {
        val KOMPOSITE = """
            namespace com.soywiz.klock {
                primitive DateTime
            }
            namespace net.akehurst.kaf.technology.persistence.neo4j {
                datatype AddressBook {
                  val  title : String
                  car  contacts : Map<String, Contact>
                }
                datatype Contact {
                  val  alias : String
                  var  name : String
                  var  email : String
                  var  phone : Set<PhoneNumber>
                  var  dateOfBirth : DateTime
                  dis  age : TimeSpan
                  var  friendsWith : Set<Contact>
                }
                datatype PhoneNumber {
                  val label: String
                  val number: String
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
    fun create_Contact_empty() {
        this.configure()

        val c = Contact("adam")
        sut.create(Contact::class, c)
    }

    @Test
    fun create_Contact_1() {
        this.configure()

        val c = Contact("adam")
        c.name = "Adam"
        c.dateOfBirth = DateTime(year = Year(1954), month = Month.November, day = 3)
        sut.create(Contact::class, c)
    }

    @Test
    fun create_Contact_2() {
        this.configure()

        val c = Contact("adam")
        c.name = "Adam"
        c.dateOfBirth = DateTime(year = Year(1954), month = Month.November, day = 3)
        sut.create(Contact::class, c)
    }

    @Test
    fun create_AddressBook_empty() {
        this.configure()

        val abk = AddressBook("friends")
        sut.create(AddressBook::class, abk)
    }

    @Test
    fun create_AddressBook_containing_1() {
        this.configure()

        val abk = AddressBook("friends")
        val c1 = Contact("adam")
        abk.contacts.put(c1.alias, c1)

        sut.create(AddressBook::class, abk)
    }

    @Test
    fun read() {
        // given
        this.configure()
        val abk = AddressBook("friends")
        sut.create(AddressBook::class, abk)

        // when
        val filter = FilterProperty("title", "friends")
        val actual = sut.read(AddressBook::class, setOf(filter))

        // then
        val expected = abk
        assertNotNull(actual)
        assertEquals(expected, actual)
    }
}