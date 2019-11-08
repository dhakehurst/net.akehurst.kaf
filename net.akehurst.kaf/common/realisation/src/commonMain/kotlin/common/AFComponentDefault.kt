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
        initialise: suspend () -> Unit,
        execute: suspend () -> Unit,
        finalise: suspend () -> Unit
) : AFActiveDefault(self, afIdentity, initialise, execute, finalise), AFComponent {

    class Builder(val self: Component, val id: String) {
        var initialise: suspend () -> Unit = {}
        var execute: suspend () -> Unit = {}
        var finalise: suspend () -> Unit = {}
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
}

class AFPortDefault(
        val component: Component,
        val portId: String,
        val required: Map<KClass<*>, MutableSet<Any>>,
        val provided: Map<KClass<*>, MutableSet<Any>>
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

    private fun <T : Any> provideProvided(interfaceType: KClass<T>, provider: T) {
        var set: MutableSet<Any> = this.provided[interfaceType]!!
        set.add(provider)
    }

    override fun <T : Any> provided(providedInterface: KClass<T>): T {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T : Any> required(requiredInterface: KClass<T>): T {
        TODO()
    }

    override fun connectInternal(internal: Passive) {
        for (req in this.required.keys) {
            for (extConn in internal.af.externalConnections(req)) {
                try {
                    //val kclass = extConn.key.returnType.classifier as KClass<*>
                    val delegate = extConn.value as ExternalConnection<Any>
                    delegate.setValue(internal)
                    component.af.log.trace { "Internally connected ${component.af.identity}[$portId]required(${req}) to ${internal.af.identity}.${extConn.key.name}:${extConn.value.class_}" }
                } catch (ex: Exception) {
                    component.af.log.error { "Trying to connectInternal ${component.af.identity}[$portId]required(${req}) to ${internal.af.identity}.${extConn.key.name}" }
                }
            }
        }

        for (prov in this.provided.keys) {
            if (prov.isInstance(internal)) {
                this.provideProvided(prov as KClass<Any>, internal)
                component.af.log.trace { "Internally connected ${component.af.identity}[$portId]provided(${prov}) to ${internal.af.identity}:${internal::class}." }
            }
        }
    }

    override fun connectInternal(internal: Port) {
        /*
        for (intf in this.provided.keys) {
            val providers = internal.provided(intf) as Set<Any>
            for (provider in providers) {
                val t = intf as KClass<Any>
                this.provideProvided<Any>(t, provider)
                this.logger.log(LogLevel.TRACE, "Internally connected %s[%s] to %s.", this.afId(), t.getName(), provider.toString())
            }
        }

        for (intf in internal.required) {
            val t = intf as Class<Any>
            val provider = this.out<*>(intf)
            internalPort.provideRequired(t, provider)
            this.logger.log(LogLevel.TRACE, "Internally connected %s[%s] to %s.", this.afId(), t.getName(), provider.toString())
        }
         */
    }

    override fun connect(other: Port) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun connect(other: Passive) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}