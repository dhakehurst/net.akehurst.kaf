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

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.akehurst.kaf.common.api.*
import net.akehurst.kaf.service.api.Service
import net.akehurst.kaf.service.api.serviceReference
import net.akehurst.kaf.service.logging.api.logger
import kotlin.reflect.KClass

actual inline fun afApplication(self: Application, id: String, init: AFApplicationDefault.Builder.() -> Unit): AFApplication {
    val builder = AFApplicationDefault.Builder(self, id)
    builder.init()
    return builder.build()
}

actual class AFApplicationDefault(
        override val self: Application,
        override val identity: String,
        val defineServices: Map<KClass<*>, (commandLineArgs: List<String>) -> Service>,
        val initialise: suspend (self:Application) -> Unit,
        val execute: suspend (self:Application) -> Unit,
        val finalise: suspend (self:Application) -> Unit
) : AFApplication {

    actual class Builder(val self: Application, val id: String) {
        val _defineServices = mutableMapOf<KClass<*>, (commandLineArgs: List<String>) -> Service>()

        actual var initialise: suspend (self:Application) -> Unit = {}
        actual var execute: suspend (self:Application) -> Unit = {}
        actual var finalise: suspend (self:Application) -> Unit = {}
        actual inline fun <reified T : Service> defineService(serviceClass: KClass<T>, noinline func: (commandLineArgs: List<String>) -> T) {
            _defineServices[serviceClass] = func
        }

        actual fun build(): AFApplication {
            return AFApplicationDefault(self, id, _defineServices, initialise, execute, finalise)
        }
    }

    val framework by serviceReference<ApplicationFrameworkService>()
    val log by logger("logging")

    private val _services = mutableMapOf<KClass<*>, Service>()
    override fun <T : Service> service(serviceClass: KClass<T>): T {
        return this._services[serviceClass] as T? ?: throw ApplicationInstantiationException("Service not found for $serviceClass")
    }

    private fun defineAndInject(commandLineArgs: List<String>) {
        this.defineServices.forEach { me ->
            this._services[me.key] = me.value(commandLineArgs)
        }
        val fws = ApplicationFrameworkServiceDefault("${this.identity}.framework", self, this._services)
        this._services[ApplicationFrameworkService::class] = fws
        fws.doInjections(commandLineArgs, this.self)
    }

    private suspend fun start() {
        log.trace { "start" }
        val activeParts = this.framework.partsOf(self).filterIsInstance<Active>()
        activeParts.forEach {
            it.af.start()
        }
        this.execute(self)
        activeParts.forEach {
            it.af.join()
        }
    }

    override fun startAsync(commandLineArgs: List<String>) {
        defineAndInject(commandLineArgs)
        runBlocking {
            this.initialise(self)
        }
        GlobalScope.launch {
            start()
        }
    }

    override fun startBlocking(commandLineArgs: List<String>) {
        defineAndInject(commandLineArgs)
        runBlocking {
            this.initialise(self)
        }
        runBlocking {
            start()
        }
    }

    override fun shutdown() {
        log.trace { "shutdown begin" }
        runBlocking {
            val activeParts = this.framework.partsOf(self).filterIsInstance<Active>()
            activeParts.forEach {
                it.af.shutdown()
               // it.af.join()
            }
            this.finalise(self)
        }
        log.trace { "shutdown end" }
    }

    override fun terminate() {
        runBlocking {
            log.trace { "terminate" }
            val activeParts = this.framework.partsOf(self).filterIsInstance<Active>()
            activeParts.forEach {
                it.af.terminate()
            }
        }
    }
}