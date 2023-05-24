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
import java.io.File
import java.lang.StringBuilder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.reflect.KClass

@Suppress("ExtractKtorModule")
class WebserverKtor<SessionDataType : Any>(
    //val sessionType: KClass<T>,
    val createDefaultSession: (sessionId: String) -> SessionDataType,
    val sessionId: (SessionDataType) -> String,
    val serialiseSession: (sessionData: SessionDataType) -> String,
    val deserialiseSession: (serialised: String) -> SessionDataType
) : Component, MessageChannel<SessionDataType>, Webserver {

    companion object {
        interface SessionIdContainer{ val sessionId:SessionId}
        const val SESSION_SERIALISE_SEPARATOR = "$"
        inline class SessionId(val value:String)
        class Session(val id: SessionId) {
            constructor(id:SessionId, data: Any): this(id) {this.data = data}
            lateinit var data:Any
            override fun hashCode(): Int = id.hashCode()
            override fun equals(other: Any?): Boolean = when (other) {
                !is Session -> false
                else -> this.id == other.id
            }

            override fun toString(): String = "Session($id)"
        }

        fun Path.deleteRecursive() {
            when {
                this.isDirectory() -> {
                    this.listDirectoryEntries().forEach { it.deleteRecursive() }
                    this.deleteIfExists()
                }
                else ->  this.deleteIfExists()
            }
        }
    }

    lateinit var port_server: Port
    lateinit var port_comms: Port

    private val port: Int by configuredValue { 9090 }
    private val sessionStorageDirectoryName: String by configuredValue { "./sessions" }
    private val sessionStorageDirectory: File by lazy {
        val path = Path.of(sessionStorageDirectoryName)
        if(path.exists()) path.deleteRecursive()
        Files.createDirectory(path).toFile()
    }
    val messageChannel: MessageChannel<SessionDataType> by externalConnection()

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
                    cookie<Session>("SESSION", directorySessionStorage(sessionStorageDirectory)) {
                        this.serializer = object : SessionSerializer<Session> {
                            override fun deserialize(text: String): Session {
                                val id = text.substringBefore(SESSION_SERIALISE_SEPARATOR)
                                val data = deserialiseSession(text.substringAfter(SESSION_SERIALISE_SEPARATOR))
                                return Session(SessionId(id),data)
                            }

                            override fun serialize(session: Session): String {
                                val b = StringBuilder()
                                b.append(session.id.value)
                                b.append(SESSION_SERIALISE_SEPARATOR)
                                val sds = serialiseSession(session.data as SessionDataType)
                                b.append(sds)
                                return b.toString()
                            }

                        }
                    }
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
                        val sessionId = SessionId(generateNonce())
                        val sessionData = createDefaultSession(sessionId.value) //sessionType.primaryConstructor!!.call(generateNonce())
                        val session = Session(sessionId, sessionData)
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
                this.filesPath = pathToResources
                this.defaultPage = defaultPage
                this.useResources = useFilesNotResource.not()
            }
        }
        af.log.debug { "added SPA route at '$spaRoute' serving '${pathToResources}' from ${if (useFilesNotResource) "files" else "resources"}, defaultPage='$defaultPage'" }
    }

    // --- MessageChannel ---

    private val connections = mutableMapOf<SessionId, WebSocketServerSession>()
    private val receiveActions = mutableMapOf<ChannelIdentity, suspend (sessionData: SessionDataType, message: String) -> Unit>()

    private suspend fun handleWebsocketConnection(ws: WebSocketServerSession) {
        val n = ws.call.sessions.findName(Session::class)
        val session = ws.call.sessions.get(n) as Session?
        if (session == null) {
            //this should not happen
            ws.close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No session"))
        } else {
            connections[session.id] = ws
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
                                        this.receiveActions[channelId]?.invoke(session.data as SessionDataType, message)
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
                connections.remove(session.id)
                af.log.trace { "Websocket Connection closed from $session" }
            }
        }
    }

    override fun <T : Any> receiveAll(interfaceToReceive: KClass<T>, target: T) {
        TODO("not implemented")
    }

    override fun receive(channelId: ChannelIdentity, action: suspend (endPointId: SessionDataType, message: String) -> Unit) {
        this.receiveActions[channelId] = action
    }

    override fun send(endPointId: SessionDataType, channelId: ChannelIdentity, message: String) {
        val sessionId = SessionId(this.sessionId(endPointId))
        val ws = connections[sessionId] ?: throw MessageChannelException("Endpoint not found for $endPointId")
        val frame = Frame.Text("${channelId.value}${MessageChannel.DELIMITER}${message}")
        ws.outgoing.trySend(frame)
    }

}