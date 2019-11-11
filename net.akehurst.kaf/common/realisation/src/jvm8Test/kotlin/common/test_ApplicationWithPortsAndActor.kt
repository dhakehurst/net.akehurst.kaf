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

class test_ApplicationWithPortsAndActor {

    interface Output {
        suspend fun writeln(text: String?)
    }

    class component_Greeter(override val owner: Owner, afId: String) : Component {

        lateinit var port_display: Port
        val handler = actor_Greeter(this, "handler")

        override val af = afComponent(this, afId) {
            port_display = port("display") { requires(Output::class) }
            initialise = {
                port_display.connectInternal(handler)
            }
        }
    }

    class actor_Greeter(override val owner: Owner, afId: String) : Actor {
        interface Shutdown {
            fun shutdown()
        }

        val framework by serviceReference<ApplicationFrameworkService>()
        val output: Output by externalConnection()

        val confGreeting: String by configuredValue("greeting") { "unknown" }
        val greeting: String? by commandLineValue() { confGreeting }

        override val af = afActor(this, afId) {
            preExecute = {
                output.writeln(greeting)
                self.af.receive(Shutdown::shutdown, asyncCallContext()) {
                    framework.shutdown()
                }
            }
        }

    }

    class component_Console(override val owner: Owner, afId: String) : Component {

        lateinit var port_output: Port
        val handler = actor_Console(this, "handler")

        override val af = afComponent(this, afId) {
            port_output = port("output") { provides(Output::class) }
            initialise = {
                port_output.connectInternal(handler)
            }
        }
    }

    class actor_Console(override val owner: Owner, afId: String) : Actor, Output {
        override val af = afActor(this, afId)

        override suspend fun writeln(text: String?) {
            println(text)
        }
    }

    class TestApplication(afId: String) : Application {

        val greeter = component_Greeter(this, "greeter")
        val console = component_Console(this, "console")

        override val af = afApplication(this, afId) {
            defineService(ConfigurationService::class) {
                ConfigurationMap(mutableMapOf(
                        "sut.greeter.handler.greeting" to "Hello World!"
                ))
            }
            defineService(LoggingService::class) { LoggingServiceConsole(LogLevel.ALL) }
            defineService(CommandLineHandlerService::class) { commandLineArgs -> CommandLineHandlerSimple(commandLineArgs) }

            initialise = {
                greeter.port_display.connect(console.port_output)
            }
        }
    }


    @Test
    fun test() {

        val sut = TestApplication("sut")
        sut.af.startBlocking(listOf())

    }

}
