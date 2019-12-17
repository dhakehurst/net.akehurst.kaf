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

package net.akehurst.kaf.technology.persistence.hjson

import com.soywiz.korio.file.std.uniVfs
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
import net.akehurst.kaf.technology.persistence.fs.api.PersistenceFilesystem
import java.lang.RuntimeException
import kotlin.test.*

class test_PersistentStoreFilesystemHJson : Application {

    companion object {
        val KOMPOSITE = """
            namespace com.soywiz.klock {
                primitive DateTime
                primitive TimeSpan
            }
            namespace net.akehurst.kaf.technology.persistence.hjson {
                datatype A {
                  val prop : String
                }
            }
        """.trimIndent()
    }

    //val tempDir = createTempDir("_temp")

    override val af = afApplication(this, "test") {
        defineService(LoggingService::class) { LoggingServiceConsole(LogLevel.ALL) }
        defineService(CommandLineHandlerService::class) { commandLineArgs -> CommandLineHandlerSimple(commandLineArgs) }
        defineService(ConfigurationService::class) {
            ConfigurationMap(mutableMapOf(

            ))
        }
        initialise = {
            sut.fs = fs
        }
    }

    val sut = PersistentStoreHJsonOverFilesystem()
    val fs = object : PersistenceFilesystem {
        val map = mutableMapOf<String,ByteArray>()
        override fun write(uri: String, bytes: ByteArray) {
            println("Writing to fs at : $uri")
            println(uri.uniVfs.absolutePath)
            println(String(bytes))
            map[uri] = bytes
        }

        override fun read(uri: String): ByteArray {
            println("Reading from : $uri")
            println(uri.uniVfs.absolutePath)
            return map[uri] ?: throw RuntimeException("uri not found")
        }

        override fun read(uri: String, offset: Long, size: Int): Sequence<ByteArray> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    @BeforeTest
    fun startup() {
        this.af.startAsync(listOf())
    }

    @AfterTest
    fun shutdown() {
        this.af.shutdown()
        //while (tempDir.exists()) {
        //    println("deleting ${tempDir.absoluteFile}")
        //    tempDir.deleteRecursively()
        //}
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

    @ExperimentalStdlibApi
    @Test
    fun create() {
        this.configure()

        val a = A("a")
        sut.create(A::class, a)  { identity }
    }

    @ExperimentalStdlibApi
    @Test
    fun createAll() {
        this.configure()

        val a1 = A("a1")
        val a2 = A("a2")
        val a3 = A("a3")
        val a4 = A("a4")
        val setOfA = setOf(a1, a2, a3, a4)
        sut.createAll(A::class, setOfA) { identity }
    }

    @ExperimentalStdlibApi
    @Test
    fun read() {
        // given
        this.configure()
        val a = A("a")
        sut.create(A::class, a)  { identity }

        // when
        val actual = sut.read(A::class, "a")

        // then
        val expected = a
        assertNotNull(actual)
        assertEquals(expected, actual)
    }

    @ExperimentalStdlibApi
    @Test
    fun readAll() {
        // given
        this.configure()

        val a1 = A("a1")
        val a2 = A("a2")
        val a3 = A("a3")
        val a4 = A("a4")
        val setOfA = setOf(a1, a2, a3, a4)
        sut.createAll(A::class, setOfA)  { identity }

        // when
        val actual = sut.readAll(A::class, setOf("a1","a2","a3","a4"))

        // then
        val expected = setOfA
        assertNotNull(actual)
        assertEquals(expected, actual)
    }
}