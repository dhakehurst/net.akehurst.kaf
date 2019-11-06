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
import net.akehurst.kaf.common.api.AFApplication
import net.akehurst.kaf.common.api.Active
import net.akehurst.kaf.common.api.Application
import net.akehurst.kaf.common.api.ApplicationFrameworkService
import net.akehurst.kaf.service.api.Service
import kotlin.reflect.KClass

actual inline fun afApplication(self: Application, id: String, init: AFApplicationDefault.Builder.() -> Unit): AFApplication {
    val builder = AFApplicationDefault.Builder(self, id)
    builder.init()
    return builder.build()
}

actual class AFApplicationDefault(
        override val self: Application,
        afIdentity: String,
        val defineServices: Map<KClass<*>, (commandLineArgs: List<String>) -> Service>,
        val initialise: suspend () -> Unit,
        val execute: suspend () -> Unit,
        val terminate: suspend () -> Unit
) : AFPassiveDefault(self, afIdentity), AFApplication {

    actual class Builder(val self: Application, val id: String) {
        val _defineServices = mutableMapOf<KClass<*>, (commandLineArgs: List<String>) -> Service>()

        actual var initialise: suspend () -> Unit = {}
        actual var execute: suspend () -> Unit = {}
        actual var terminate: suspend () -> Unit = {}
        actual inline fun <reified T : Service> defineService(serviceClass: KClass<T>, noinline func: (commandLineArgs: List<String>) -> T) {
            _defineServices[serviceClass] = func
        }

        actual fun build(): AFApplication {
            return AFApplicationDefault(self, id, _defineServices, initialise, execute, terminate)
        }
    }

    private val _services = mutableMapOf<KClass<*>, Service>()
    val services: Map<KClass<*>, Service> get() = this._services

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
        this@AFApplicationDefault.initialise()

        val activeParts = super.framework.partsOf(self).filterIsInstance<Active>()
        activeParts.forEach {
            it.af.start()
        }

        this@AFApplicationDefault.execute()

        activeParts.forEach {
            it.af.join()
        }

    }

    override fun startAsync(commandLineArgs: List<String>) {
        defineAndInject(commandLineArgs)
        GlobalScope.launch {
            start()
        }
    }

    override fun startBlocking(commandLineArgs: List<String>) {
        defineAndInject(commandLineArgs)
        runBlocking {
            start()
        }
    }

    override fun shutdown() {
        GlobalScope.launch {
            log.trace { "shutdown" }
            val activeParts = super.framework.partsOf(self).filterIsInstance<Active>()
            activeParts.forEach {
                it.af.shutdown()
            }
        }
    }

    override fun terminate() {
        runBlocking {
            log.trace { "terminate" }
            val activeParts = super.framework.partsOf(self).filterIsInstance<Active>()
            activeParts.forEach {
                it.af.terminate()
            }
        }
    }
}