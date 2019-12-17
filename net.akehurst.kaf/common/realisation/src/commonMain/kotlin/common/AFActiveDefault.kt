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
import net.akehurst.kotlinx.reflect.proxyFor
import net.akehurst.kotlinx.reflect.reflect
import kotlin.reflect.KClass


inline fun afActive(selfIdentity: String? = null, init: AFActiveDefault.Builder.() -> Unit = {}): AFActive {
    val builder = AFActiveDefault.Builder(selfIdentity)
    builder.init()
    return builder.build()
}

open class AFActiveDefault(
        selfIdentity: String? = null,
        val initialiseBlock: suspend (self: Active) -> Unit,
        val executeBlock: suspend (self: Active) -> Unit,
        val finaliseBlock: suspend (self: Active) -> Unit
) : AFDefault(selfIdentity), AFActive {

    class Builder(val selfIdentity: String?) {
        var initialise: suspend (self: Active) -> Unit = {}
        var execute: suspend (self: Active) -> Unit = {}
        var finalise: suspend (self: Active) -> Unit = {}
        fun build(): AFActive {
            return AFActiveDefault(selfIdentity, initialise, execute, finalise)
        }
    }

    override val self: Active get() = super.self()

    override var owner: AFOwner? = null
    val ownerIdentity: String
        get() {
            val o = owner
            return if (null == o) {
                ""
            } else {
                o.identity + "."
            }
        }

    override val identity: String
        get() = "${ownerIdentity}$selfIdentity"

    override suspend fun initialise() {
        log.trace { "initialise parts" }
        val parts = super.framework.partsOf(self).filterIsInstance<Passive>()
        parts.forEach {
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

    override fun <T : Any> receiver(forInterface: KClass<T>): T {
        return proxyFor(forInterface) { handler, proxy, callable, methodName, args ->
            when {
                forInterface.isInstance(self) -> self.reflect().call(callable.name, *args)
                else -> throw ActiveException("${self.af.identity}:${self::class.simpleName!!} does not implement ${forInterface.simpleName!!}")
            }
        }
    }
}