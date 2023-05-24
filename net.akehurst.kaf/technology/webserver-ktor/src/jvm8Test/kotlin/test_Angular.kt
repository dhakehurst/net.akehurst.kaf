/**
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.kaf.technology.webserver.ktor

import net.akehurst.kaf.common.api.Actor
import net.akehurst.kaf.common.api.Application
import net.akehurst.kaf.common.realisation.afActor
import net.akehurst.kaf.common.realisation.afApplication
import net.akehurst.kaf.service.commandLineHandler.api.CommandLineHandlerService
import net.akehurst.kaf.service.commandLineHandler.simple.CommandLineHandlerSimple
import net.akehurst.kaf.service.configuration.api.ConfigurationService
import net.akehurst.kaf.service.configuration.map.ServiceConfigurationMap
import net.akehurst.kaf.service.logging.api.LogLevel
import net.akehurst.kaf.service.logging.api.LoggingService
import net.akehurst.kaf.service.logging.console.LoggingServiceConsole

fun main() {
    val app = test_Angular("test")
    app.af.startBlocking(emptyList())
}

private class test_Angular(
    afId: String
) : Application {

    companion object {
        data class TestSession(
            val sessionId: String
        )
    }

    val actor = object : Actor {
        override val af = afActor {

        }
    }

    val webserver = WebserverKtor<TestSession>(
        createDefaultSession = { TestSession(it) },
        sessionId = { it.sessionId },
        serialiseSession = { sess -> "" },
        deserialiseSession = { sess -> TestSession(sess) }
    )

    override val af = afApplication(this, afId) {
        defineService(LoggingService::class) { LoggingServiceConsole(LogLevel.ALL) }
        defineService(CommandLineHandlerService::class) { commandLineArgs -> CommandLineHandlerSimple(commandLineArgs) }
        defineService(ConfigurationService::class) {
            ServiceConfigurationMap(
                mutableMapOf(
                    "test.webserver.port" to 9999
                )
            )
        }
        execute = {
            webserver.addSinglePageApplication("/dist","")
        }
    }
}