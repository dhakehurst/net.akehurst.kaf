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

package net.akehurst.kaf.technology.messageChannel.inMemory

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import net.akehurst.kaf.common.api.Active
import net.akehurst.kaf.common.api.Component
import net.akehurst.kaf.common.api.Port
import net.akehurst.kaf.common.realisation.AsyncCallContextDefault
import net.akehurst.kaf.common.realisation.afActive
import net.akehurst.kaf.common.realisation.afComponent
import net.akehurst.kaf.technology.messageChannel.api.ChannelIdentity
import net.akehurst.kaf.technology.messageChannel.api.MessageChannel
import net.akehurst.kotlinx.reflect.reflect
import kotlin.reflect.KClass

class component_MessageChannelInMemory<T : Any>(
        val constructEndPointId: (String) -> T
) : Component {

    lateinit var port_endPoint1:Port
    lateinit var port_endPoint2:Port

    val endPoint1 = MessageChannelInMemoryEndPoint<T>(constructEndPointId)
    val endPoint2 = MessageChannelInMemoryEndPoint<T>(constructEndPointId)

    override val af = afComponent {
        port_endPoint1 = port("endPoint1") {
            provides(MessageChannel::class)
        }
        port_endPoint2 = port("endPoint2") {
            provides(MessageChannel::class)
        }
        initialise = {
            port_endPoint1.connectInternal(endPoint1)
            port_endPoint2.connectInternal(endPoint2)
            endPoint1.outgoing = endPoint2.incoming
            endPoint2.outgoing = endPoint1.incoming
        }
    }

}


class MessageChannelInMemoryEndPoint<T : Any>(
        val constructEndPointId: (String) -> T
) : Active, MessageChannel<T> {

    lateinit var outgoing: Channel<String>
    val incoming = Channel<String>()

    override val af = afActive {
        execute = { self ->
            val asyncCallContext = AsyncCallContextDefault(self.af.identity)
            GlobalScope.launch(asyncCallContext) {
                incoming.consumeEach { frame ->
                    val endPointIdText = frame.substringBefore(MessageChannel.DELIMITER)
                    val endPointId = constructEndPointId(endPointIdText)
                    val channelIdMessage = frame.substringAfter(MessageChannel.DELIMITER)
                    val channelId = ChannelIdentity(channelIdMessage.substringBefore(MessageChannel.DELIMITER))
                    val message = channelIdMessage.substringAfter(MessageChannel.DELIMITER)
                    receiveActions[channelId]?.invoke(endPointId, message)
                }
            }
        }
    }

    // --- MessageChannel ---
    private val receiveActions = mutableMapOf<ChannelIdentity, suspend (endPointId: T, message: String) -> Unit>()

    override fun <T : Any> receiveAll(interfaceToReceive: KClass<T>, target: T) {
        TODO() //interfaceToReceive.reflect().
    }

    override fun receive(channelId: ChannelIdentity, action: suspend (endPointId: T, message: String) -> Unit) {
        this.receiveActions[channelId] = action
    }

    override fun send(endPointId: T, channelId: ChannelIdentity, message: String) {
        val frame = "$endPointId${MessageChannel.DELIMITER}$channelId${MessageChannel.DELIMITER}$message"
        GlobalScope.launch {
            outgoing.send(frame)
        }
    }
}