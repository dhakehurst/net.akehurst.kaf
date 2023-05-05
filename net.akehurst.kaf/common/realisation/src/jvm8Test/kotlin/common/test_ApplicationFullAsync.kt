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

package net.akehurst.kaf.common.realisation

import net.akehurst.kaf.common.api.*
import net.akehurst.kaf.service.commandLineHandler.api.CommandLineHandlerService

import net.akehurst.kaf.service.commandLineHandler.api.commandLineValue
import net.akehurst.kaf.service.commandLineHandler.simple.CommandLineHandlerSimple
import net.akehurst.kaf.service.configuration.api.ConfigurationService
import net.akehurst.kaf.service.configuration.api.configuredValue
import net.akehurst.kaf.service.configuration.map.ServiceConfigurationMap
import net.akehurst.kaf.service.logging.api.LogLevel
import net.akehurst.kaf.service.logging.api.LoggingService
import net.akehurst.kaf.service.logging.console.LoggingServiceConsole
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime


class test_ApplicationFullAsync {

    interface OutputRequest {
        suspend fun writeln(text: String?)
    }

    interface OutputNotification {
        suspend fun doneWriteln(text: String?)
    }

    class Greeter() : Actor, OutputNotification {
        interface Shutdown {
            fun shutdown()
        }

        lateinit var output: OutputRequest

        val confGreeting: String by configuredValue("greeting") { "unknown" }
        val greeting: String? by commandLineValue() { confGreeting }

        @ExperimentalTime
        override val af = afActor() {
            preExecute = { self ->
                self.af.send {
                    output.writeln(greeting)
                }.andWhen(OutputNotification::doneWriteln, 5.seconds) { txt: String? ->
                    self.af.framework.shutdown()
                }.go()
            }
        }

        override suspend fun doneWriteln(text: String?) {
            throw RuntimeException("Should never do this as the 'andWhen' should take precedence")
        }
    }

    class Console() : Actor, OutputRequest {
        lateinit var outputNotification: OutputNotification

        override val af = afActor()

        override suspend fun writeln(text: String?) {
            println(text)
            outputNotification.doneWriteln(text)
        }
    }

    class TestApplication(afId: String) : Application {

        val greeter = Greeter()
        val console = Console()

        @ExperimentalTime
        override val af = afApplication(this,afId) {
            defineService(ConfigurationService::class) {
                ServiceConfigurationMap(mutableMapOf(
                        "sut.greeter.greeting" to "Hello World!"
                ))
            }
            defineService(LoggingService::class) { LoggingServiceConsole(LogLevel.ALL) }
            defineService(CommandLineHandlerService::class) { commandLineArgs -> CommandLineHandlerSimple(commandLineArgs) }

            initialise = {
                greeter.output = console.af.receiver(OutputRequest::class)
                console.outputNotification = greeter.af.receiver(OutputNotification::class)
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
