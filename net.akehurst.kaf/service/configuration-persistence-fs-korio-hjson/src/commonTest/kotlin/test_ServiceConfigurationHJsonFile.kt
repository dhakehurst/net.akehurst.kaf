package net.akehurst.kaf.service.configuration.hjson

import net.akehurst.hjson.HJsonDocument
import net.akehurst.hjson.hjson
import net.akehurst.kaf.common.api.AFPassive
import net.akehurst.kaf.common.api.Application
import net.akehurst.kaf.common.api.Owner
import net.akehurst.kaf.common.api.Passive
import net.akehurst.kaf.common.realisation.afApplication
import net.akehurst.kaf.common.realisation.afPassive
import net.akehurst.kaf.service.configuration.api.ConfigurationService
import net.akehurst.kaf.service.configuration.api.configuredValue
import net.akehurst.kaf.service.logging.api.LogLevel
import net.akehurst.kaf.service.logging.api.LoggingService
import net.akehurst.kaf.service.logging.console.LoggingServiceConsole
import net.akehurst.kaf.technology.persistence.fs.api.PersistenceFilesystem
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class test_ServiceConfigurationHJsonFile : Application {

    class Obj() : Passive {
        val confValue : String by configuredValue { "default" }
        override val af = afPassive()
    }
    val obj = Obj()

    var confDoc = hjson("confDoc") {
        objectJson {  }
    }

    @ExperimentalStdlibApi
    val fs = object : PersistenceFilesystem {
        override fun write(uri: String, bytes: ByteArray) {
            TODO("not implemented")
        }

        override fun read(uri: String): ByteArray {
            return confDoc.toHJsonString().encodeToByteArray()
        }

        override fun read(uri: String, offset: Long, size: Int): Sequence<ByteArray> {
            TODO("not implemented")
        }
    }

    @ExperimentalStdlibApi
    override val af = afApplication(this,"test") {
        defineService(LoggingService::class) { LoggingServiceConsole(LogLevel.ALL) }
        val cs = ServiceConfigurationHJsonFile("application.hjson", true)
        cs.fs= fs
        defineService(ConfigurationService::class) { cs }

    }

    @ExperimentalStdlibApi
    @BeforeTest
    fun setup() {
        af.startAsync(emptyList())
    }

    @Test
    fun defaultValue() {
        confDoc = hjson("confDoc") {
            objectJson {  }
        }
        assertEquals("default", this.obj.confValue)
    }

    @Test
    fun fromConfFile() {
        confDoc = hjson("confDoc") {
            objectJson {
                property("obj") {
                    objectJson {
                        property("confValue", "fromConfFile")
                    }
                }
            }
        }
        assertEquals( "fromConfFile", this.obj.confValue)
    }

}