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

import net.akehurst.kaf.common.api.Application
import net.akehurst.kaf.common.realisation.afApplication
import net.akehurst.kaf.service.commandLineHandler.api.CommandLineHandlerService
import net.akehurst.kaf.service.commandLineHandler.simple.CommandLineHandlerSimple
import net.akehurst.kaf.service.configuration.api.ConfigurationService
import net.akehurst.kaf.service.configuration.map.ServiceConfigurationMap
import net.akehurst.kaf.service.logging.api.LogLevel
import net.akehurst.kaf.service.logging.api.LoggingService
import net.akehurst.kaf.service.logging.console.LoggingServiceConsole
import kotlin.io.path.createTempDirectory
import kotlin.test.*

class test_PersystentStoreNeo4j_withAny : Application {

    companion object {
        val KOMPOSITE = """
            namespace net.akehurst.kaf.technology.persistence.neo4j {
                datatype ContainsAnAnyProp {
                  composite-val number : Int
                  composite-val something: Any?
                }
                datatype A {
                  composite-val prop : String
                }
            }
        """.trimIndent()
    }

    val tempDir = createTempDirectory(".neo4j_").toFile()

    override val af = afApplication(this, "test") {
        defineService(LoggingService::class) { LoggingServiceConsole(LogLevel.ALL) }
        defineService(CommandLineHandlerService::class) { commandLineArgs -> CommandLineHandlerSimple(commandLineArgs) }
        defineService(ConfigurationService::class) {
            ServiceConfigurationMap(mutableMapOf(
                    "test.sut.embeddedNeo4jDirectory" to tempDir.absoluteFile.toString()
            ))
        }
    }

    val sut = PersistentStoreNeo4j()

    @BeforeTest
    fun startup() {
        kaf_technology_persistence_neo4j_commonTest.KotlinxReflectForModule.registerUsedClasses()
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
        this.sut.configure(mapOf(
                "embedded" to true,
                "uri" to "bolt://localhost:7777",
                "user" to "neo4j",
                "password" to "neo4j",
                "komposite" to listOf(KOMPOSITE)
        ))
    }

    @Test
    fun create() {
        this.configure()

        val obj = ContainsAnAnyProp(7, "Hello")
        sut.create(ContainsAnAnyProp::class, obj) { identity }
    }

    @Test
    fun createAll() {
        this.configure()

        val a1 = ContainsAnAnyProp(1, "Hello")
        val a2 = ContainsAnAnyProp(2, A("xyz"))
        val a3 = ContainsAnAnyProp(3, setOf(1,2,3))
        val a4 = ContainsAnAnyProp(4, ContainsAnAnyProp(5, "World") )
        val setOfObj = setOf(a1, a2, a3, a4)
        sut.createAll(ContainsAnAnyProp::class, setOfObj) { identity }
    }

    @Test
    fun read() {
        // given
        this.configure()
        val obj = ContainsAnAnyProp(7, "Hello")
        sut.create(ContainsAnAnyProp::class, obj) { identity }

        // when
        val actual = sut.read(ContainsAnAnyProp::class, "id7")

        // then
        val expected = obj
        assertNotNull(actual)
        assertEquals(expected, actual)
    }

    @Test
    fun readAllIdentity() {
        // given
        this.configure()
        val a1 = ContainsAnAnyProp(1, "Hello")
        val a2 = ContainsAnAnyProp(2, A("xyz"))
        val a3 = ContainsAnAnyProp(3, setOf(1,2,3))
        val a4 = ContainsAnAnyProp(4, ContainsAnAnyProp(5, "World") )
        val setOfObj = setOf(a1, a2, a3, a4)
        sut.createAll(ContainsAnAnyProp::class, setOfObj) { identity }

        // when
        val actual = sut.readAllIdentity(ContainsAnAnyProp::class)

        // then
        val expected = setOf("id1", "id2", "id3", "id4", "id4/something")
        assertNotNull(actual)
        assertEquals(expected, actual)
    }

    @Test
    fun readAll_withIds() {
        // given
        this.configure()
        val a1 = ContainsAnAnyProp(1, "Hello")
        val a2 = ContainsAnAnyProp(2, A("xyz"))
        val a3 = ContainsAnAnyProp(3, setOf(1,2,3))
        val a41 = ContainsAnAnyProp(41, "World")
        val a4 = ContainsAnAnyProp(4, a41 )
        val a5 = ContainsAnAnyProp(5, null )
        val setOfObj = setOf(a1, a2, a3, a4, a5)
        sut.createAll(ContainsAnAnyProp::class, setOfObj) { identity }

        // when
        val actual = sut.readAll(ContainsAnAnyProp::class, setOf("id1", "id4", "id5"))

        // then
        val expected = setOf(a1, a4, a5)
        assertNotNull(actual)
        assertEquals(expected, actual)
    }

}