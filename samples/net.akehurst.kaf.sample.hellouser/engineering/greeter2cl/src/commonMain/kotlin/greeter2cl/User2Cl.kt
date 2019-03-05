package net.akehurst.kaf.simple.hellouser.engineering.greeter2cl

import net.akehurst.kaf.sample.hellouser.greeter.api.Credentials
import net.akehurst.kaf.sample.hellouser.greeter.api.GreeterNotification
import net.akehurst.kaf.sample.hellouser.greeter.api.GreeterRequest
import net.akehurst.kaf.sample.hellouser.greeter.api.Message
import net.akehurst.kaf.simple.hellouser.technology.cl.api.Console

class Greeter2Cl : GreeterNotification {

    lateinit var greeterRequest: GreeterRequest

    lateinit var console: Console

    override fun started() {
        console.stdout.write("Started")
        val username = console.environment.variable["user"] ?: "<unknown>"

        greeterRequest.authenticate(Credentials(username, "<unknown>"))
    }

    override fun sendMessage(message: Message) {
        console.stdout.write(message.value)
    }
}