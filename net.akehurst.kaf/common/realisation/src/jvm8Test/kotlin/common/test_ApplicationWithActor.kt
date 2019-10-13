package net.akehurst.kaf.common.realisation

import net.akehurst.kaf.common.api.*
import net.akehurst.kaf.service.api.serviceReference
import net.akehurst.kaf.service.commandLineHandler.api.CommandLineHandlerService

import net.akehurst.kaf.service.commandLineHandler.api.commandLineValue
import net.akehurst.kaf.service.commandLineHandler.simple.CommandLineHandlerSimple
import net.akehurst.kaf.service.configuration.api.ConfigurationService
import net.akehurst.kaf.service.configuration.api.configuredValue
import net.akehurst.kaf.service.configuration.map.ConfigurationMap
import net.akehurst.kaf.service.logging.api.LogLevel
import net.akehurst.kaf.service.logging.api.LoggingService
import net.akehurst.kaf.service.logging.console.LoggingServiceConsole
import kotlin.test.Test

class test_ApplicationWithActor {

    interface Output {
        fun writeln(text: String?)
    }

    class Greeter(afId: String) : Actor {

        val framework by serviceReference<ApplicationFrameworkService>()
        lateinit var output: Output

        val confGreeting: String by configuredValue("greeting") { "unknown" }
        val greeting: String? by commandLineValue() { confGreeting }

        override val af = afActor(this, afId) {
            preExecute = {
                output.writeln(greeting)
                self.af.receive("shutdown") {
                    framework.shutdown()
                }
            }
        }


    }

    class Console(afId: String) : Actor, Output {
        override val af = afActor(this, afId)

        override fun writeln(text: String?) {
            println(text)
        }
    }

    class TestApplication(afId: String) : Application {

        val greeter = Greeter("$afId.greeter")
        val console = Console("$afId.console")

        override val af = afApplication(this, afId) {
            defineService(ConfigurationService::class) {
                ConfigurationMap(mutableMapOf(
                        "sut.greeter.greeting" to "Hello World!"
                ))
            }
            defineService(LoggingService::class) { LoggingServiceConsole(LogLevel.ALL) }
            defineService(CommandLineHandlerService::class) { commandLineArgs -> CommandLineHandlerSimple(commandLineArgs) }

            initialise = {
                greeter.output = console.af.receiver(Output::class)
            }

            execute = {
            }
            terminate = {
            }
        }
    }


    @Test
    fun test() {

        val sut = TestApplication("sut")
        sut.af.startBlocking(listOf("--sut.greeter.greeting='Hello Jim'"))

    }

}