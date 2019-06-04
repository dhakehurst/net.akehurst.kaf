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

import net.akehurst.kaf.api.Active
import net.akehurst.kaf.common.*

import net.akehurst.kaf.sample.hellouser.greeter.api.Credentials
import net.akehurst.kaf.sample.hellouser.greeter.api.GreeterNotification
import net.akehurst.kaf.sample.hellouser.greeter.api.GreeterRequest
import net.akehurst.kaf.sample.hellouser.greeter.api.Message
import net.akehurst.kaf.service.commandLineHandler.api.commandLineValue
import net.akehurst.kaf.service.configuration.api.configuredValue
import net.akehurst.kaf.service.logging.api.LogLevel

class GreeterSimple( afIdentity:String )
    : GreeterRequest, Active
{

    val confGreeting: String? by configuredValue("configuration","greeting") {"Hello"}
    val greeting: String? by commandLineValue("cmdLine","greeting") {confGreeting}

    override  val af = afActive(this,afIdentity) {
        execute = {}
    }

    lateinit var out: GreeterNotification

    override fun start() {
        af.logger.log(LogLevel.INFO, {"start"})
        out.started()
    }

    override fun authenticate(credentials:Credentials) {
        out.sendMessage( Message("$greeting ${credentials.username}") )
    }
}