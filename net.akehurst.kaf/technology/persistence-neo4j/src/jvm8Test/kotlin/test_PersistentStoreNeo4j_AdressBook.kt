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

package net.akehurst.kaf.technology.persistence.neo4j

import com.soywiz.klock.DateTime
import com.soywiz.klock.Month
import com.soywiz.klock.Year
import net.akehurst.kaf.common.api.Application
import net.akehurst.kaf.common.realisation.afApplication
import net.akehurst.kaf.service.commandLineHandler.api.CommandLineHandlerService
import net.akehurst.kaf.service.commandLineHandler.simple.CommandLineHandlerSimple
import net.akehurst.kaf.service.configuration.api.ConfigurationService
import net.akehurst.kaf.service.configuration.map.ConfigurationMap
import net.akehurst.kaf.service.logging.api.LogLevel
import net.akehurst.kaf.service.logging.api.LoggingService
import net.akehurst.kaf.service.logging.console.LoggingServiceConsole
import net.akehurst.kaf.technology.persistence.api.FilterProperty
import net.akehurst.kotlin.komposite.api.PrimitiveMapper
import kotlin.reflect.KClass
import kotlin.test.*

class test_PersystentStoreNeo4j_AddressBook : Application {

    companion object {
        val KOMPOSITE = """
            namespace com.soywiz.klock {
                primitive DateTime
                primitive TimeSpan
            }
            namespace net.akehurst.kaf.technology.persistence.neo4j {
                datatype AddressBook {
                  val  title : String
                  car  contacts : Map<String, Contact>
                }
                datatype Contact {
                  val  alias : String
                  var  name : String
                  var  emails : List<String>
                  car  phone : Set<PhoneNumber>
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
        defineService(LoggingService::class) { LoggingServiceConsole(LogLevel.ALL) }
        defineService(CommandLineHandlerService::class) { commandLineArgs -> CommandLineHandlerSimple(commandLineArgs) }
        defineService(ConfigurationService::class) {
            ConfigurationMap(mutableMapOf(
                    "test.sut.embeddedNeo4jDirectory" to tempDir.absoluteFile.toString()
            ))
        }
        initialise = {}
    }

    val sut = PersistentStoreNeo4j()

    @BeforeTest
    fun startup() {
        this.af.startAsync(listOf())
    }

    @AfterTest
    fun shutdown() {
        this.af.shutdown()
        while (tempDir.exists()) {
            println("deleting ${tempDir.absoluteFile}")
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun configure() {
        val primitiveMappers = mutableMapOf<KClass<*>, PrimitiveMapper<*,*>>()
        this.sut.configure(mapOf(
                "embedded" to true,
                "uri" to "bolt://localhost:7777",
//                "embedded" to false,
//                "uri" to "bolt://localhost:7687",
                "user" to "neo4j",
                "password" to "admin",
                "komposite" to listOf(KOMPOSITE),
                "primitiveMappers" to primitiveMappers
        ))
    }

    @Test
    fun create_Contact_empty() {
        // given
        this.configure()
        val c = Contact("adam")

        //when
        sut.create(Contact::class, c) { alias }

        //then
        //TODO
    }

    @Test
    fun create_Contact_1() {
        // given
        this.configure()
        val c = Contact("adam")
        c.name = "Adam Ant"
        c.dateOfBirth = DateTime(year = Year(1954), month = Month.November, day = 3)

        //when
        sut.create(Contact::class, c) { alias }

        //then
        //TODO
    }

    @Test
    fun create_Contact_2() {
        //given
        this.configure()
        val c = Contact("adam")
        c.name = "Adam Ant"
        c.dateOfBirth = DateTime(year = Year(1954), month = Month.November, day = 3)
        c.emails = mutableListOf("adam@pop.com", "adam.ant@pop.com")

        //when
        sut.create(Contact::class, c) { alias }

        //then
        //TODO
    }

    @Test
    fun create_AddressBook_empty() {
        this.configure()

        val abk = AddressBook("friends")
        sut.create(AddressBook::class, abk) { title }
    }

    @Test
    fun create_AddressBook_containing_1() {
        this.configure()

        val abk = AddressBook("friends")
        val c1 = Contact("adam")
        abk.contacts.put(c1.alias, c1)

        sut.create(AddressBook::class, abk) { title }
    }

    @Test
    fun read_empty() {
        // given
        this.configure()
        val abk = AddressBook("friends")
        sut.create(AddressBook::class, abk) { title }

        // when
        val actual = sut.read(AddressBook::class, "friends")

        // then
        val expected = abk
        assertNotNull(actual)
        assertEquals(expected, actual)
        assertEquals(expected.title, actual.title)
        assertEquals(expected.contacts.size, actual.contacts.size)
        assertEquals(expected.contacts, actual.contacts)
    }

    @Test
    fun read_containing_1() {
        // given
        this.configure()
        val abk = AddressBook("friends")
        val c1 = Contact("adam")
        c1.emails.add("adam@pop.com")
        c1.emails.add("adam.ant@pop.com")
        c1.dateOfBirth = DateTime(year = 1972, month = Month.November, day = 21)
        c1.name = "Adam Ant"
        c1.phone.add(PhoneNumber("home", "12432523523"))
        c1.phone.add(PhoneNumber("work", "09876543123"))
        abk.contacts.put(c1.alias, c1)
        sut.create(AddressBook::class, abk) { title }

        // when
        val actual = sut.read(AddressBook::class, "friends")

        // then
        val expected = abk
        assertNotNull(actual)
        assertEquals(expected, actual)
        assertEquals(expected.title, actual.title)
        assertEquals(expected.contacts, actual.contacts)
    }

    @Test
    fun read_containing_2() {
        // given
        this.configure()
        val abk = AddressBook("friends")
        val c1 = Contact("adam")
        c1.emails.add("adam@pop.com")
        c1.emails.add("adam.ant@pop.com")
        c1.dateOfBirth = DateTime(year = 1972, month = Month.November, day = 21)
        c1.name = "Adam Ant"
        c1.phone.add(PhoneNumber("home", "12432523523"))
        c1.phone.add(PhoneNumber("work", "09876543123"))
        abk.contacts.put(c1.alias, c1)
        val c2 = Contact("brian")
        abk.contacts.put(c2.alias, c2)
        sut.create(AddressBook::class, abk) { title }

        // when
        val actual = sut.read(AddressBook::class, "friends")

        // then
        val expected = abk
        assertNotNull(actual)
        assertEquals(expected, actual)
        assertEquals(expected.title, actual.title)
        assertEquals(expected.contacts, actual.contacts)
        expected.contacts.forEach { me ->
            val exp = me.value
            val act = actual.contacts[me.key]!!
            assertEquals(exp, act)
            assertEquals(exp.age, act.age)
            assertEquals(exp.alias, act.alias)
            assertEquals(exp.dateOfBirth, act.dateOfBirth)
            assertEquals(exp.emails, act.emails)
            assertEquals(exp.friendsWith, act.friendsWith)
            assertEquals(exp.name, act.name)
            assertEquals(exp.phone, act.phone)
        }
    }
}