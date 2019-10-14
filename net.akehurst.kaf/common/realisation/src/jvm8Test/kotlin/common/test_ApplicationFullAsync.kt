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
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

class test_ApplicationFullAsync {

    interface OutputRequest {
        suspend fun writeln(text: String?)
    }

    interface OutputNotification {
        suspend fun doneWriteln(text: String?)
    }

    class Greeter(afId: String) : Actor, OutputNotification {
        interface Shutdown {
            fun shutdown()
        }

        val framework by serviceReference<ApplicationFrameworkService>()
        lateinit var output: OutputRequest

        val confGreeting: String by configuredValue("greeting") { "unknown" }
        val greeting: String? by commandLineValue() { confGreeting }

        @ExperimentalTime
        override val af = afActor(this, afId) {
            preExecute = {
                self.af.send {
                    output.writeln(greeting)
                }.andWhen(OutputNotification::doneWriteln, 5.seconds) { txt:String? ->
                    framework.shutdown()
                }.go()
            }
        }

        override suspend fun doneWriteln(text: String?) {
            throw RuntimeException("Should never do this as the 'andWhen' should take precedence")
        }
    }

    class Console(afId: String) : Actor, OutputRequest {
        lateinit var outputNotification: OutputNotification

        override val af = afActor(this, afId)

        override suspend fun  writeln(text: String?) {
            println(text)
            outputNotification.doneWriteln(text)
        }
    }

    class TestApplication(afId: String) : Application {

        val greeter = Greeter("$afId.greeter")
        val console = Console("$afId.console")

        @ExperimentalTime
        override val af = afApplication(this, afId) {
            defineService(ConfigurationService::class) {
                ConfigurationMap(mutableMapOf(
                        "sut.greeter.greeting" to "Hello World!"
                ))
            }
            defineService(LoggingService::class) { LoggingServiceConsole(LogLevel.ALL) }
            defineService(CommandLineHandlerService::class) { commandLineArgs -> CommandLineHandlerSimple(commandLineArgs) }

            initialise = {
                greeter.output = console.af.receiver(OutputRequest::class)
                console.outputNotification = greeter.af.receiver(OutputNotification::class)
            }

            execute = {
            }
            terminate = {
            }
        }
    }


    @ExperimentalTime
    @Test
    fun test() {

        val sut = TestApplication("sut")
        sut.af.startBlocking(listOf("--sut.greeter.greeting='Hello Jim'"))

    }

}
