package net.akehurst.kaf.service.commandLineHandler.clikt

import com.github.ajalt.clikt.core.NoRunCliktCommand
import net.akehurst.kaf.api.Active
import net.akehurst.kaf.api.Application
import net.akehurst.kaf.api.CompositePart
import net.akehurst.kaf.common.afActive
import net.akehurst.kaf.common.afApplication

import net.akehurst.kaf.service.commandLineHandler.api.commandLineValue
import net.akehurst.kaf.service.logging.api.LogLevel
import net.akehurst.kaf.service.logging.console.LoggingServiceConsole
import kotlin.test.Test

class test_Application {

    class TestApplication(id:String) : Application {

        @CompositePart
        val comp = object : Active {
            val greeting:String? by commandLineValue("cmdLineHandler", "greeting") { "unknown" }

            override  val af = afActive(this, "comp") {
                execute = {
                    self.af.log.info {greeting}
                }
            }
        }

        override val af = afApplication(this, id) {
            defineServices = { commandLineArgs ->
                mapOf(
                        "logger" to LoggingServiceConsole(LogLevel.ALL),
                        "cmdLineHandler" to CommandLineHandlerClikt(
                                commandLineArgs,
                                NoRunCliktCommand(name="test")
                        )
                )
            }
            execute = {
                comp.af.start()
            }
        }
    }


    @Test
    fun help() {

        val sut = TestApplication("sut")
        sut.af.start(listOf("--help"))

    }

    @Test
    fun greeting() {

        val sut = TestApplication("sut")
        sut.af.start(listOf("--greeting=hi"))

    }
}
