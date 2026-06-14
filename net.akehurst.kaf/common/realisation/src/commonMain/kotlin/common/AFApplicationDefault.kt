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

package net.akehurst.kaf.common.realisation

import net.akehurst.kaf.common.api.*
import net.akehurst.kaf.service.api.Service
import net.akehurst.kaf.service.logging.api.Logger
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

expect inline fun afApplication(self: Application, identity: String, init: AFApplicationDefault.Builder.() -> Unit = {}): AFApplication

expect class AFApplicationDefault : AFApplication {
    class Builder {
        inline fun <reified T : Service> defineService(serviceClass: KClass<T>, noinline func: (commandLineArgs: List<String>) -> T)
        var initialise: suspend (self: Application) -> Unit
        var execute: suspend (self: Application) -> Unit
        var finalise: suspend (self: Application) -> Unit
        fun build(): AFApplication
    }

    // -- AF --
    override var selfIdentity: String?
    override var afHolder: AFHolder?
    override val identity: String
    override val framework: ApplicationFrameworkService
    override val log: Logger
    override fun externalConnections(klass: KClass<*>): Map<KProperty<*>, ExternalConnection<*>>
    override fun doInjections(root: Passive)

    // -- AFApplication --
    override val self: Application

    override fun <T : Service> service(serviceClass: KClass<T>): T
    override fun startAsync(commandLineArgs: List<String>)
    override fun startBlocking(commandLineArgs: List<String>)
    override fun shutdown()
    override fun terminate()
}
