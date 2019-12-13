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

package net.akehurst.kaf.service.commandLineHandler.clikt

import net.akehurst.kaf.common.api.Active
import net.akehurst.kaf.common.api.Application
import net.akehurst.kaf.common.realisation.afActive
import net.akehurst.kaf.common.realisation.afApplication
import net.akehurst.kaf.service.commandLineHandler.api.CommandLineHandlerService
import net.akehurst.kaf.service.commandLineHandler.api.commandLineValue
import net.akehurst.kaf.service.logging.api.LogLevel
import net.akehurst.kaf.service.logging.api.LoggingService
import net.akehurst.kaf.service.logging.console.LoggingServiceConsole
import kotlin.test.Test

class test_Application {

    class ActivePart() : Active {
        val greeting: String? by commandLineValue() { "greeting" }

        override val af = afActive {
            execute = { self ->
                self.af.log.info { greeting }
            }
        }
    }

    class TestApplication(id: String) : Application {

        val comp: Active = ActivePart()

        override val af = afApplication(this, id) {
            defineService(LoggingService::class) { LoggingServiceConsole(LogLevel.ALL) }
            defineService(CommandLineHandlerService::class) { commandLineArgs -> CommandLineHandlerClikt(commandLineArgs) }
            execute = {
                comp.af.start()
            }
        }
    }


    @Test
    fun help() {

        val sut = TestApplication("sut")
        sut.af.startBlocking(listOf("--help"))

    }

    @Test
    fun greeting() {

        val sut = TestApplication("sut")
        sut.af.startBlocking(listOf("--comp.greeting=hi"))

    }
}
