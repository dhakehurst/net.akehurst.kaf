package net.akehurst.kaf.engineering.channel.genericMessageChannel.test.computational

import net.akehurst.kaf.common.api.Component
import net.akehurst.kaf.common.api.Port
import net.akehurst.kaf.common.realisation.afComponent
import net.akehurst.kaf.common.realisation.asyncSend

inline class Message(val value: String)

data class Credentials(
        val username: String,
        val password: String
)

interface UserRequest {
    suspend fun requestLogin(sessionId: String, creds: Credentials)
}

interface UserNotification {
    suspend fun notifyLoginSuccess(sessionId: String, message: Message)
    suspend fun notifyLoginFailure(sessionId: String, message: Message)
}


class Core : Component, UserRequest {

    lateinit var port_user: Port

    override val af = afComponent {
        port_user = port("user") {
            contract(provides = UserRequest::class, requires = UserNotification::class)
        }
        initialise = { self ->
            port_user.connectInternal(self)
        }
    }

    // --- UserRequest ---

    override suspend fun requestLogin(sessionId: String, creds: Credentials) {
        if ("user" == creds.username) {
            port_user.forRequired(UserNotification::class).notifyLoginSuccess(sessionId, Message("OK"))
        } else {
            port_user.forRequired(UserNotification::class).notifyLoginFailure(sessionId, Message("User Unknown"))
        }
    }
}


class Gui : Component, UserNotification {

    lateinit var port_core: Port

    override val af = afComponent {
        port_core = port("core") {
            contract(provides = UserNotification::class, requires = UserRequest::class)
        }
        initialise = { self ->
            port_core.connectInternal(self)
        }
        execute = {
            // normally triggered by explicit UI action
            asyncSend {
                port_core.forRequired(UserRequest::class).requestLogin("abcdefg", Credentials("user2", "pwd"))
            }
        }
    }

    // --- UserNotification ---

    override suspend fun notifyLoginSuccess(sessionId: String, message: Message) {
        println("login success: ${message.value}")
        this.af.framework.shutdown() //test ends here
    }

    override suspend fun notifyLoginFailure(sessionId: String, message: Message) {
        println("login failure: ${message.value}")
        this.af.framework.shutdown()  //test ends here
    }
}