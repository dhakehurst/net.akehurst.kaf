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


import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.application.ApplicationCallPipeline.ApplicationPhase.Plugins
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.websocket.*
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import net.akehurst.kaf.common.api.*
import net.akehurst.kaf.common.realisation.afComponent
import net.akehurst.kaf.common.realisation.asyncSend
import net.akehurst.kaf.service.configuration.api.configuredValue
import net.akehurst.kaf.technology.messageChannel.api.ChannelIdentity
import net.akehurst.kaf.technology.messageChannel.api.MessageChannel
import net.akehurst.kaf.technology.messageChannel.api.MessageChannelException
import net.akehurst.kaf.technology.webserver.api.Webserver
import org.slf4j.event.*
import java.time.Duration
import kotlin.reflect.KClass

@Suppress("ExtractKtorModule")
class WebserverKtor<SessionType : Any>(
    //val sessionType: KClass<T>,
    val creaateDefaultSession: (nonce: String) -> SessionType
) : Component, MessageChannel<SessionType>, Webserver {

    companion object {
        data class Session<T>(val id:String) {
           var data:T?=null
        }
    }

    lateinit var port_server: Port
    lateinit var port_comms: Port

    private val port: Int by configuredValue { 9090 }

    val messageChannel: MessageChannel<SessionType> by externalConnection()

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
                install(DefaultHeaders) {
                    header("X-Engine", "Ktor") // will send this header with each response
                }
                install(CallLogging) {
                    level = Level.INFO
                    filter { call -> call.request.path().startsWith("/") }
                }
                install(Routing)
                install(Sessions) {
                    cookie<Session<*>>("SESSION")
                }
                install(WebSockets) {
                    pingPeriod = Duration.ofSeconds(15)
                    timeout = Duration.ofSeconds(15)
                    maxFrameSize = Long.MAX_VALUE
                    masking = false
                }
                intercept(Plugins) {
                    // create session if one does not exist already
                    val n = call.sessions.findName(Session::class)
                    if (call.sessions.get(n) == null) {
                        val sessionId = generateNonce()
                        val sessionData = creaateDefaultSession(sessionId) //sessionType.primaryConstructor!!.call(generateNonce())
                        val session = Session<SessionType>(sessionId).also{ it.data = sessionData }
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
            server.stop(0, 0)
        }
    }

    fun addTextRoute(path: String, text: String) {
        this.server.application.routing {
            get(path) {
                println("textRoute")
                call.respondText(text, ContentType.Text.Plain)
            }
        }
    }

    fun addStaticResourcesRoute(routePath: String, resourcePath: String, defaultFile: String = "index.html") {
        this.server.application.routing {
            //staticResources(resourcePath)
            //staticFiles()
        }.static(routePath) {
            resources(resourcePath)
            default(defaultFile)
        }
    }

    fun addSinglePageApplication(pathToResources: String, spaRoute: String = "", defaultPage: String = "index.html", useFilesNotResource: Boolean = false) {
        //this.server.application.install(SinglePageApplication) {
        //    this.folderPath = pathToResources
        //    this.spaRoute = spaRoute
        //    this.defaultPage = defaultPage
        //    this.useFiles = useFilesNotResource
        //}
        this.server.application.routing {
            singlePageApplication {
                this.applicationRoute = spaRoute
                this.filesPath=pathToResources
                this.defaultPage=defaultPage
                this.useResources=useFilesNotResource.not()
            }
        }
    }

    // --- MessageChannel ---

    private val connections = mutableMapOf<SessionType, WebSocketServerSession>()
    private val receiveActions = mutableMapOf<ChannelIdentity, suspend (endPointId: SessionType, message: String) -> Unit>()

    private suspend fun handleWebsocketConnection(ws: WebSocketServerSession) {
        val n = ws.call.sessions.findName(Session::class)
        val session = ws.call.sessions.get(n) as SessionType?
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
                                } catch (t: Throwable) {
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

    override fun receive(channelId: ChannelIdentity, action: suspend (endPointId: SessionType, message: String) -> Unit) {
        this.receiveActions[channelId] = action
    }

    override fun send(endPointId: SessionType, channelId: ChannelIdentity, message: String) {
        val ws = connections[endPointId] ?: throw MessageChannelException("Endpoint not found for $endPointId")
        val frame = Frame.Text("${channelId.value}${MessageChannel.DELIMITER}${message}")
        ws.outgoing.trySend(frame)
    }

}