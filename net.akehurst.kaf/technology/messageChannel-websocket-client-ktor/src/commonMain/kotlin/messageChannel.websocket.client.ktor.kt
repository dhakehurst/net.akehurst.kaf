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

package net.akehurst.kaf.technology.messageChannel.websocket.client.ktor

import io.ktor.client.HttpClient
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.ws
import io.ktor.http.HttpMethod
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.WebSocketSession
import io.ktor.http.cio.websocket.readText
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import net.akehurst.kaf.common.api.Active
import net.akehurst.kaf.common.api.Owner
import net.akehurst.kaf.common.realisation.afActive
import net.akehurst.kaf.technology.messageChannel.api.ChannelIdentity
import net.akehurst.kaf.technology.messageChannel.api.MessageChannel
import kotlin.js.JsName
import kotlin.reflect.KClass

class MessageChannelWebsocketKtor<T : Any>(
        val endPointId: T,
        val host: String,
        val port: Int,
        val path: String
) : Active, MessageChannel<T> {

    @JsName("websocket")
    private var websocket: WebSocketSession? = null
    private val receiveActions = mutableMapOf<ChannelIdentity, suspend (endPointId: T, message: String) -> Unit>()

    private suspend fun initWS() {
        val client = HttpClient() {
            install(WebSockets)
        }
        client.ws(
                method = HttpMethod.Get,
                host = host,
                port = port,
                path = path
        ) {
            handleWebsocketConnection(endPointId, this)
        }
    }

    override val af = afActive {
        execute = { self ->
            initWS()
        }
    }

    @JsName("afStart")
    fun afStart() {
        //TODO: can't support af on JS until more reflection support for kotlin-JS provided
        GlobalScope.launch {
            initWS()
        }
    }

    // --- MessageChannel ---

    private suspend fun handleWebsocketConnection(session:T, ws: WebSocketSession) {
        websocket = ws
        println( "Websocket Connection opened from $session" )
        //af.log.trace { "Websocket Connection opened from $session" }
        try {
            ws.incoming.consumeEach { frame ->
                println("Websocket Connection message from $session, $frame" )
                //af.log.trace { "Websocket Connection message from $session, $frame" }
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        val channelId = ChannelIdentity(text.substringBefore(MessageChannel.DELIMITER))
                        val message = text.substringAfter(MessageChannel.DELIMITER)
                        println( "Message ${channelId.value}: $message" )
                        if (this.receiveActions.containsKey(channelId)) {
                            try {
                                this.receiveActions[channelId]?.invoke(session, message)
                            } catch (t:Throwable) {
                                //af.log.error(t) { "Error invoking action $channelId" }
                                println( "Error invoking action $channelId: $t")
                            }
                        } else {
                            //af.log.error { "Websocket Connection message from $session, $frame" }
                            println { "No action registered for $channelId" }
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
            websocket = null
            println( "Websocket Connection closed from $session" )
            //af.log.trace { "Websocket Connection closed from $session" }
        }
    }

    override fun <T : Any> receiveAll(interfaceToReceive: KClass<T>, target: T) {
        TODO("not implemented")
    }

    override fun receive(channelId: ChannelIdentity, action: suspend (endPointId: T, message: String) -> Unit) {
        this.receiveActions[channelId] = action
    }

    override fun send(endPointId: T, channelId: ChannelIdentity, message: String) {
        val frame = Frame.Text("${channelId.value}${MessageChannel.DELIMITER}${message}")
        websocket?.outgoing?.offer(frame)
    }


}