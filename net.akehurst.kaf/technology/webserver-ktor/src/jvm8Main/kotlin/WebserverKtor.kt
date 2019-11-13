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
import io.ktor.application.featureOrNull
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.DefaultHeaders
import io.ktor.http.ContentType
import io.ktor.http.content.default
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.websocket.WebSockets
import net.akehurst.kaf.common.api.AFComponent
import net.akehurst.kaf.common.api.Component
import net.akehurst.kaf.common.api.Owner
import net.akehurst.kaf.common.api.Port
import net.akehurst.kaf.common.realisation.afComponent
import net.akehurst.kaf.service.configuration.api.configuredValue
import net.akehurst.kaf.technology.comms.api.MessageChannel
import net.akehurst.kaf.technology.webserver.api.Webserver
import java.util.concurrent.TimeUnit

class WebserverKtor(
        override val owner: Owner,
        afId: String
) : Component {

    lateinit var port_server:Port
    lateinit var port_comms:Port

    private val port: Int by configuredValue { 9090 }

    lateinit var messageChannel: MessageChannel<*>

    private lateinit var server: ApplicationEngine

    override val af: AFComponent = afComponent(this, afId) {
        port_server = port("server") {
            provides(Webserver::class)
        }
        port_comms = port("comms") {
            contract(provides = MessageChannel::class, requires = MessageChannel::class)

        }

        initialise = {
            self.af.log.info { "port = $port" }
            server = embeddedServer(Netty, port = port) {
                install(DefaultHeaders)
                install(CallLogging)
                install(Routing)
                install(WebSockets)
            }
        }
        execute = {
            server.start(false)
        }
        finalise = {
            server.stop(0,0,TimeUnit.SECONDS)
        }
    }

    fun addTextRoute(path:String, text:String) {
        this.server.application.featureOrNull(Routing)?.get(path) {
            call.respondText(text, ContentType.Text.Plain)
        }
    }

    fun addStaticResourcesRoute(routePath:String, resourcePath:String, defaultFile:String="index.html") {
        this.server.application.featureOrNull(Routing)?.static(routePath) {
            resources(resourcePath)
            default(defaultFile)
        }
    }
}