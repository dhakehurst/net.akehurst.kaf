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
import net.akehurst.kotlinx.collections.MapNonNull
import net.akehurst.kotlinx.collections.mutableMapNonNullOf
import net.akehurst.kotlinx.reflect.reflect
import kotlin.reflect.KClass

inline fun afComponent(self: Component, id: String, init: AFComponentDefault.Builder.() -> Unit = {}): AFComponent {
    val builder = AFComponentDefault.Builder(self, id)
    builder.init()
    return builder.build()
}


open class AFComponentDefault(
        afIdentity: String,
        override val self: Component,
        override val port: MapNonNull<String, Port>,
        val initialiseBlock: suspend (self:Component) -> Unit,
        val executeBlock: suspend (self:Component) -> Unit,
        val finaliseBlock: suspend (self:Component) -> Unit
) : AFPassiveDefault(self, afIdentity), AFComponent {

    class Builder(val self: Component, val id: String) {
        var initialise: suspend (self:Component) -> Unit = {}
        var execute: suspend (self:Component) -> Unit = {}
        var finalise: suspend (self:Component) -> Unit = {}
        val ports = mutableMapNonNullOf<String, Port>()

        fun port(portId: String, init: AFPortDefault.Builder.() -> Unit): Port {
            val builder = AFPortDefault.Builder(self, portId)
            builder.init()
            val port = builder.build()
            ports[portId] = port
            return port
        }

        fun build(): AFComponent {
            return AFComponentDefault(id, self, ports, initialise, execute, finalise)
        }
    }

    override fun <T : Any> portOut(requiredInterface: KClass<T>): T {
        TODO()
    }

    override fun <T : Any> portIn(providedInterface: KClass<T>): T {
        TODO()
    }

    override suspend fun initialise() {
        log.trace { "initialise activeParts" }
        val activeParts = super.framework.partsOf(self).filterIsInstance<Active>()
        activeParts.forEach {
            it.af.initialise()
        }

        log.trace { "initialise" }
        this.initialiseBlock(this.self)
    }

    override suspend fun start() {
        log.trace { "start" }

        val activeParts = super.framework.partsOf(self).filterIsInstance<Active>()
        activeParts.forEach {
            it.af.start()
        }

        log.trace { "execute" }
        this.executeBlock(this.self)

    }

    override suspend fun join() {
        log.trace { "join" }
        val activeParts = super.framework.partsOf(self).filterIsInstance<Active>()
        activeParts.forEach {
            it.af.join()
        }
    }

    override suspend fun shutdown() {
        log.trace { "shutdown" }
        val activeParts = super.framework.partsOf(self).filterIsInstance<Active>()
        activeParts.forEach {
            it.af.shutdown()
        }
        log.trace { "finalise" }
        this.finaliseBlock(this.self)
    }

    override suspend fun terminate() {
        log.trace { "terminate" }
        val activeParts = super.framework.partsOf(self).filterIsInstance<Active>()
        activeParts.forEach {
            it.af.terminate()
        }
    }

    override fun <T : Any> receiver(forInterface:KClass<T>): T {
        return super.framework.proxy(forInterface) { handler, proxy, callable, args ->
            when {
                forInterface.isInstance(self) -> self.reflect().call(callable.name, *args)
                else -> throw ActiveException("${self.af.identity}:${self::class.simpleName!!} does not implement ${forInterface.simpleName!!}")
            }
        }
    }
}

class AFPortDefault(
        val component: Component,
        val portId: String,
        override val required: Map<KClass<*>, MutableSet<Any>>,
        override val provided: Map<KClass<*>, MutableSet<Any>>
) : Port {
    class Builder(
            val component: Component,
            val portId: String
    ) {
        private val required = mutableSetOf<KClass<*>>()
        private val provided = mutableSetOf<KClass<*>>()

        fun contract(provides: KClass<*>? = null, requires: KClass<*>? = null) {
            if (null != requires) {
                requires(requires)
            }
            if (null != provides) {
                provides(provides)
            }
        }

        fun requires(requiredInterface: KClass<*>) = required.add(requiredInterface)
        fun provides(providedInterface: KClass<*>) = provided.add(providedInterface)

        fun build(): Port {
            val req = this.required.associate { Pair(it, mutableSetOf<Any>()) }
            val prv = this.provided.associate { Pair(it, mutableSetOf<Any>()) }
            return AFPortDefault(this.component, this.portId, req, prv)
        }
    }

    private fun <T : Any> outProxy(forInterface: KClass<T>): T {
        return this.component.af.framework.proxy(forInterface) { handler, proxy, callable, args ->
            //TODO: really want directMembers of forInterface only
            when {
                Any::equals == callable -> handler.reflect().call(callable.name, *args)
                Any::hashCode == callable -> handler.reflect().call(callable.name, *args)
                Any::toString == callable -> "outProxy for $this"
                else -> {
                    component.af.log.trace { "calling ${callable.name}" }
                    var result: Any? = null
                    this.required(forInterface).forEach {
                        if (it is Active) {
                            val rec = it.af.receiver(forInterface)
                            result = rec.reflect().call(callable.name, *args)
                        } else {
                            result = it.reflect().call(callable.name, *args)
                        }
                    }
                    result
                }
            }
        }
    }

    private fun <T : Any> inProxy(forInterface: KClass<T>): T {
        return this.component.af.framework.proxy(forInterface) { handler, proxy, callable, args ->
            //TODO: really want directMembers of forInterface only
            when {
                Any::equals == callable -> handler.reflect().call(callable.name, *args)
                Any::hashCode == callable -> handler.reflect().call(callable.name, *args)
                Any::toString == callable -> "inProxy for $this"
                else -> {
                    component.af.log.trace { "calling ${callable.name}" }
                    var result: Any? = null
                    this.provided(forInterface).forEach {
                        if (it is Active) {
                            val rec = it.af.receiver(forInterface)
                            result = rec.reflect().call(callable.name, *args)
                        } else {
                            result = it.reflect().call(callable.name, *args)
                        }
                    }
                    result
                }
            }
        }
    }

    override fun <T : Any> provideProvided(interfaceType: KClass<out T>, provider: T) {
        var set = this.provided[interfaceType]!!
        set.add(provider)
    }

    override fun <T : Any> provideRequired(interfaceType: KClass<out T>, provider: T) {
        var set = this.required[interfaceType]!!
        set.add(provider)
    }

    override fun <T : Any> provided(providedInterface: KClass<T>): Set<T> {
        return this.provided[providedInterface] as Set<T>? ?: emptySet()
    }

    override fun <T : Any> required(requiredInterface: KClass<T>): Set<T> {
        return this.required[requiredInterface] as Set<T>? ?: emptySet()
    }

    override fun connectInternal(internal: Passive) {
        for (req in this.required.keys) {
            for (extConn in internal.af.externalConnections(req)) {
                try {
                    val delegate = extConn.value as ExternalConnection<Any>
                    val outProxy = this.outProxy(req)
                    delegate.setValue(outProxy)
                    component.af.log.trace { "${this}-{${req.simpleName}}-( internally required by ${internal.af.identity}.${extConn.key.name}:${extConn.value.class_.simpleName}" }
                } catch (ex: Exception) {
                    component.af.log.error { "Trying to connectInternal ${this}-{${req.simpleName}}-( to ${internal.af.identity}.${extConn.key.name}:${extConn.value.class_.simpleName}" }
                }
            }
        }

        for (prov in this.provided.keys) {
            if (prov.isInstance(internal)) {
                this.provideProvided(prov, internal)
                component.af.log.trace { "${this}-{${prov.simpleName}}-o internally provided by ${internal.af.identity}:${internal::class.simpleName}" }
            }
        }
    }

    override fun connectInternal(internal: Port) {
        for (prov in this.provided.keys) {
            val providers = internal.provided(prov)
            for (provider in providers) {
                this.provideProvided(prov, provider)
                component.af.log.trace { "${this}-{${prov.simpleName}}-o internally provided by $provider" }
            }
        }

        for (req in internal.required.keys) {
            val provider = this.outProxy(req)
            internal.provideRequired(req, provider)
            component.af.log.trace { "${this}-{${req.simpleName}}-( internally required by $provider" }
        }

    }

    override fun connect(other: Port) {
        for (req in this.required.keys) {
            val objs = other.provided(req)
            for (o in objs) {
                this.provideRequired(req, o)
                component.af.log.trace { "${this}-${req.simpleName}-(o-${o}" }
            }
        }

        for (req in other.required.keys) {
            val objs = this.provided(req)
            for (o in objs) {
                other.provideRequired(req, o)
                component.af.log.trace { "${this}-${req.simpleName}-o)-${o}" }
            }
        }
    }

    override fun connect(other: Passive) {
        for (req in this.provided.keys) {
            for (extConn in other.af.externalConnections(req)) {
                try {
                    val delegate = extConn.value as ExternalConnection<Any>
                    val inProxy = this.inProxy(req)
                    delegate.setValue(inProxy)
                    component.af.log.trace { "Connected ${other}.${extConn.key.name} to $this" }
                } catch (ex: Exception) {
                    component.af.log.error { "Trying to connect ${other}.${extConn.key.name} to $this" }
                }
            }
        }

        for (prov in this.required.keys) {
            if (prov.isInstance(other)) {
                this.provideRequired(prov, other)
                component.af.log.trace { "Connected ${this}provided($prov) to $other." }
            }
        }
    }

    override fun toString(): String = "${component.af.identity}[$portId]"
}