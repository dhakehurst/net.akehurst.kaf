package net.akehurst.kaf.sample.hellouser.greeter.simple

import net.akehurst.kaf.sample.hellouser.greeter.api.Credentials
import net.akehurst.kaf.sample.hellouser.greeter.api.GreeterNotification
import net.akehurst.kaf.sample.hellouser.greeter.api.GreeterRequest
import net.akehurst.kaf.sample.hellouser.greeter.api.Message

class GreeterSimple
    : GreeterRequest
{

    lateinit var out: GreeterNotification

    override fun start() {
        out.started()
    }

    override fun authenticate(credentials:Credentials) {
        out.sendMessage( Message("Hello ${credentials.username}") )
    }
}