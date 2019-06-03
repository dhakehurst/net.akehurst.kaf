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

        val sut = GreeterSimple("sut")
        sut.out = out

        sut.start()

        assertEquals( true, started_called )

    }



    @Test
    fun authenticate() {

        val sut = GreeterSimple("sut")
        sut.out = out
        sut.start()
        assertEquals( true, started_called )

        sut.authenticate(Credentials("user", "pass"))

        assertEquals( "Hello user", arg_message?.value )

    }
}