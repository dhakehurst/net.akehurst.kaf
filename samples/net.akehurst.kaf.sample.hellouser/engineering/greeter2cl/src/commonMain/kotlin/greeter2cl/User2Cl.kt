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