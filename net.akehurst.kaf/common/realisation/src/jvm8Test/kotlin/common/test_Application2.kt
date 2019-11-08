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

import net.akehurst.kaf.common.api.Active
import net.akehurst.kaf.common.api.Application
import net.akehurst.kaf.common.api.Owner
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

class test_Application2 {

    interface Output {
        fun writeln(text: String?)
    }

    class Greeter(override val owner:Owner, afId: String) : Active {

        lateinit var output: Output

        val confGreeting: String by configuredValue("greeting") { "unknown" }
        val greeting: String? by commandLineValue() { confGreeting }

        override val af = afActive(this, afId) {
            execute = {
                output.writeln(greeting)
            }
        }
    }

    class Console(override val owner:Owner, afId: String) : Active, Output {
        override val af = afActive(this, afId)

        override fun writeln(text: String?) {
            println(text)
        }
    }

    class TestApplication(afId: String) : Application {

        val greeter = Greeter(this, "greeter")
        val console = Console(this, "console")

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
        }
    }


    @Test
    fun test() {

        val sut = TestApplication("sut")
        sut.af.startBlocking(listOf("--sut.greeter.greeting='Hello Jim'"))

    }

}
