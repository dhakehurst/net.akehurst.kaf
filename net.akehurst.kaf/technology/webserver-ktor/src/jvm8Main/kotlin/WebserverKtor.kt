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

import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.application.featureOrNull
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.DefaultHeaders
import io.ktor.http.ContentType
import io.ktor.http.cio.websocket.CloseReason
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.close
import io.ktor.http.cio.websocket.readText
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
import io.ktor.sessions.*
import io.ktor.util.generateNonce
import io.ktor.websocket.WebSocketServerSession
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import it.lamba.ktor.features.SinglePageApplication
import kotlinx.coroutines.channels.consumeEach
import net.akehurst.kaf.common.api.*
import net.akehurst.kaf.common.realisation.afComponent
import net.akehurst.kaf.common.realisation.asyncSend
import net.akehurst.kaf.service.configuration.api.configuredValue
import net.akehurst.kaf.technology.messageChannel.api.ChannelIdentity
import net.akehurst.kaf.technology.messageChannel.api.MessageChannel
import net.akehurst.kaf.technology.messageChannel.api.MessageChannelException
import net.akehurst.kaf.technology.webserver.api.Webserver
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

/**
 * sessionType must have a primary constructor that takes one string argument
 */
class WebserverKtor<T : Any>(
        val sessionType: KClass<T>,
        val creaateDefaultSession: (nonce: String) -> T
) : Component, MessageChannel<T>, Webserver {

    lateinit var port_server: Port
    lateinit var port_comms: Port

    private val port: Int by configuredValue { 9090 }

    val messageChannel: MessageChannel<T> by externalConnection()

    private lateinit var server: ApplicationEngine

    override val af: AFComponent = afComponent {
        port_server = port("server") {
            provides(Webserver::class)
        }
        port_comms = port("comms") {
            contract(provides = MessageChannel::class, requires = MessageChannel::class)

        }

        initialise = { self ->
            port_server.connectInternal(self)
            port_comms.connectInternal(self)
            self.af.log.info { "port = $port" }

        }
        execute = {
            server = embeddedServer(Netty, port = port) {
                install(DefaultHeaders)
                install(CallLogging)
                install(Routing)
                install(Sessions) {
                    cookie("SESSION", sessionType)
                }
                install(WebSockets)
                intercept(ApplicationCallPipeline.Features) {
                    // create session if one does not exist already
                    val n = call.sessions.findName(sessionType)
                    if (call.sessions.get(n) == null) {
                        val session = creaateDefaultSession(generateNonce()) //sessionType.primaryConstructor!!.call(generateNonce())
                        call.sessions.set(n, session)
                    }
                }
                routing {
                    webSocket("/ws") {//FIXME: hard coded value
                        handleWebsocketConnection(this)
                    }
                }
            }
            server.start(false)
        }
        finalise = {
            server.stop(0, 0, TimeUnit.SECONDS)
        }
    }

    fun addTextRoute(path: String, text: String) {
        this.server.application.featureOrNull(Routing)?.get(path) {
            call.respondText(text, ContentType.Text.Plain)
        }
    }

    fun addStaticResourcesRoute(routePath: String, resourcePath: String, defaultFile: String = "index.html") {
        this.server.application.featureOrNull(Routing)?.static(routePath) {
            resources(resourcePath)
            default(defaultFile)
        }
    }

    fun addSinglePageApplication(pathToResources: String, spaRoute: String = "", defaultPage: String = "index.html", useFilesNotResource: Boolean = false) {
        this.server.application.install(SinglePageApplication) {
            this.folderPath = pathToResources
            this.spaRoute = spaRoute
            this.defaultPage = defaultPage
            this.useFiles = useFilesNotResource
        }
    }

    // --- MessageChannel ---

    private val connections = mutableMapOf<T, WebSocketServerSession>()
    private val receiveActions = mutableMapOf<ChannelIdentity, suspend (endPointId: T, message: String) -> Unit>()

    private suspend fun handleWebsocketConnection(ws: WebSocketServerSession) {
        val n = ws.call.sessions.findName(sessionType)
        val session = ws.call.sessions.get(n) as T?
        if (session == null) {
            //this should not happen
            ws.close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No session"))
        } else {
            connections[session] = ws
            af.log.trace { "Websocket Connection opened from $session" }
            //messageChannel.newEndPoint(session) ?
            try {
                ws.incoming.consumeEach { frame ->
                    af.log.trace { "Websocket Connection message from $session, $frame" }
                    when (frame) {
                        is Frame.Text -> {
                            val text = frame.readText()
                            val channelId = ChannelIdentity(text.substringBefore(MessageChannel.DELIMITER))
                            val message = text.substringAfter(MessageChannel.DELIMITER)
                            af.log.trace { "Message ${channelId.value}: $message" }
                            asyncSend {
                                try {
                                    if (this.receiveActions.containsKey(channelId)) {
                                        this.receiveActions[channelId]?.invoke(session, message)
                                    } else {
                                        af.log.error { "No action registered for $channelId" }
                                    }
                                } catch (t:Throwable) {
                                    t.printStackTrace()
                                }
                            }
                        }
                        is Frame.Binary -> {
                        }
                        is Frame.Ping -> {
                        }
                        is Frame.Pong -> {
                        }
                        is Frame.Close -> {
                            // handled in finally block
                        }
                    }
                }
            } finally {
                connections.remove(session)
                af.log.trace { "Websocket Connection closed from $session" }
            }
        }
    }

    override fun <T : Any> receiveAll(interfaceToReceive: KClass<T>, target: T) {
        TODO("not implemented")
    }

    override fun receive(channelId: ChannelIdentity, action: suspend (endPointId: T, message: String) -> Unit) {
        this.receiveActions[channelId] = action
    }

    override fun send(endPointId: T, channelId: ChannelIdentity, message: String) {
        val ws = connections[endPointId] ?: throw MessageChannelException("Endpoint not found for $endPointId")
        val frame = Frame.Text("${channelId.value}${MessageChannel.DELIMITER}${message}")
        ws.outgoing.offer(frame)
    }

}