package net.akehurst.kaf.engineering.channel.genericMessageChannel.test.engineering

import net.akehurst.kaf.common.api.Actor
import net.akehurst.kaf.common.api.Component
import net.akehurst.kaf.common.api.Port
import net.akehurst.kaf.common.api.externalConnection
import net.akehurst.kaf.common.realisation.afActor
import net.akehurst.kaf.common.realisation.afComponent
import net.akehurst.kaf.engineering.channel.genericMessageChannel.test.computational.Credentials
import net.akehurst.kaf.engineering.channel.genericMessageChannel.test.computational.UserNotification
import net.akehurst.kaf.engineering.channel.genericMessageChannel.test.computational.UserRequest
import net.akehurst.kaf.engineering.genericMessageChannel.interface2MessageChannel
import net.akehurst.kaf.engineering.genericMessageChannel.messageChannel2Interface
import net.akehurst.kaf.technology.messageChannel.api.MessageChannel
import net.akehurst.kotlin.json.JsonDocument
import net.akehurst.kotlin.kserialisation.json.KSerialiserJson
import kotlin.js.JsName


class Serialiser {

    companion object {
        val KOMPOSITE = """
            namespace net.akehurst.kaf.engineering.channel.genericMessageChannel.test.computational {
                datatype Credentials {
                    val username:String
                    val password:String
                }
            }
        """.trimIndent()
    }

    internal val kserialiser = KSerialiserJson()

    init {
        this.kserialiser.confgureDatatypeModel(KOMPOSITE);
        this.kserialiser.registerKotlinStdPrimitives();
    }

    @JsName("toData")
    fun toData(jsonString: String): Any? = this.kserialiser.toData(jsonString)

    @JsName("toJson")
    fun toJson(root: Any, data: Any): JsonDocument = this.kserialiser.toJson(root, data)

}

/**
 * handles sending messages
 *  - from a 'gui' port on a UI component
 *  - to a communication channel that connects to a computational component
 *
 *  I.e. these are messages sent
 *   - from something outside 'this application' (i.e. an actual end user)
 *   - to 'this application'
 *

 */
class Gui2User : Component {
    lateinit var port_gui:Port
    lateinit var port_comms: Port

    val handler = Gui2UserHandler()

    override val af = afComponent {
        port_gui = port("gui") {
            contract(provides = UserRequest::class, requires = UserNotification::class)
        }
        port_comms = port("comms") {
            contract(provides = MessageChannel::class, requires = MessageChannel::class)
        }
        initialise = {
            port_comms.connectInternal(handler)
            port_gui.connectInternal(handler)
        }
    }
}

class Gui2UserHandlerDelegate {
    val serialiser = Serialiser()
    lateinit var channel: MessageChannel<String>
    val UserRequest = interface2MessageChannel<UserRequest, String>({ this.channel }, { args -> serialiser.toJson(args, args).toJsonString() })
}

class Gui2UserHandler(
        val delegate: Gui2UserHandlerDelegate = Gui2UserHandlerDelegate()
) : Actor, UserRequest by delegate.UserRequest {

    val userNotification: UserNotification by externalConnection()

    val channel: MessageChannel<String> by externalConnection()

    override val af = afActor {
        preExecute = {
            delegate.channel = channel
            messageChannel2Interface(userNotification, channel) { message -> delegate.serialiser.toData(message) as List<Any> }
        }
    }

}

/**
 * Handles sending messages
 *  - from a 'user' port on a computational component
 *  - to a communication channel that connects to a GUI
 *
 *  I.e. these are messages sent
 *  - from 'this application'
 *  - to something outside 'this application' (i.e. an actual end user)
 *
 *  and
 *
 * Handles receiving messages
 *  - from the communication channel (i.e. the external component)
 * and forwards them to a computational component
 *
 */
class User2Gui : Component {
    lateinit var port_user:Port
    lateinit var port_comms: Port

    val handler = User2GuiHandler()

    override val af = afComponent {
        port_user = port("user") {
            contract(provides = UserNotification::class, requires = UserRequest::class)
        }
        port_comms = port("comms") {
            contract(provides = MessageChannel::class, requires = MessageChannel::class)
        }
        initialise = {
            port_comms.connectInternal(handler)
            port_user.connectInternal(handler)
        }
    }
}

class User2GuiHandlerDelegate {
    val serialiser = Serialiser()
    lateinit var channel: MessageChannel<String>
    val UserNotification = interface2MessageChannel<UserNotification, String>({ this.channel }, { args -> serialiser.toJson(args, args).toJsonString() })
}

class User2GuiHandler(
        val delegate: User2GuiHandlerDelegate = User2GuiHandlerDelegate()
) : Actor, UserNotification by delegate.UserNotification {

    val userRequest: UserRequest by externalConnection()

    val channel: MessageChannel<String> by externalConnection()

    override val af = afActor {
        preExecute = {
            delegate.channel = channel
            messageChannel2Interface(userRequest, channel) { message -> delegate.serialiser.toData(message) as List<Any> }
        }
    }

}