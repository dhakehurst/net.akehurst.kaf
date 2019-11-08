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

package net.akehurst.kaf.technology.webserver.ktor

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.jetty.Jetty
import net.akehurst.kaf.common.api.AFComponent
import net.akehurst.kaf.common.api.Component
import net.akehurst.kaf.common.api.Owner
import net.akehurst.kaf.common.realisation.afComponent
import net.akehurst.kaf.service.configuration.api.configuredValue
import net.akehurst.kaf.technology.comms.api.MessageChannel
import net.akehurst.kaf.technology.webserver.api.Webserver
import java.util.concurrent.TimeUnit

class WebserverKtor(
        override val owner: Owner,
        afId: String
) : Component {

    private val port: Int by configuredValue { 9090 }

    lateinit var messageChannel: MessageChannel<*>

    private lateinit var server: ApplicationEngine

    override val af: AFComponent = afComponent(this, afId) {
        port("server") {
            provides(Webserver::class)
        }
        port("comms") {
            contract(provides = MessageChannel::class, requires = MessageChannel::class)

        }

        initialise = {
            self.af.log.info { "port = $port" }
            server = embeddedServer(Jetty, port = port) {
                routing {
                    get("/") {
                        call.respondText("Hello World!", ContentType.Text.Plain)
                    }
                }
            }
        }
        execute = {
            server.start(false)
        }
        finalise = {
            server.stop(0,0,TimeUnit.SECONDS)
        }
    }


}