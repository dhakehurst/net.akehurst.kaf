package net.akehurst.kaf.sample.hellouser.computational.greeter.simple

import net.akehurst.kaf.sample.hellouser.greeter.api.Credentials
import net.akehurst.kaf.sample.hellouser.greeter.api.GreeterNotification
import net.akehurst.kaf.sample.hellouser.greeter.api.Message
import kotlin.test.Test
import kotlin.test.assertEquals

class test_GreeterSimple {

    var started_called = false
    var arg_message:Message? = null
    val out = object : GreeterNotification {
        override fun started() {
            started_called = true
        }

        override fun sendMessage(message:Message) {
            arg_message = message
        }
    }

    @Test
    fun start() {

        val sut = GreeterSimple()
        sut.out = out

        sut.start()

        assertEquals( true, started_called )

    }



    @Test
    fun authenticate() {

        val sut = GreeterSimple()
        sut.out = out
        sut.start()
        assertEquals( true, started_called )

        sut.authenticate(Credentials("user", "pass"))

        assertEquals( "Hello user", arg_message?.value )

    }
}