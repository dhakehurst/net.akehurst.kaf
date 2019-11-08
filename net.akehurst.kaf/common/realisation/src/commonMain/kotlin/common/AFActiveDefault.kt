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

import net.akehurst.kaf.common.api.AFActive
import net.akehurst.kaf.common.api.AFOwner
import net.akehurst.kaf.common.api.Active
import net.akehurst.kaf.common.api.ActiveException
import net.akehurst.kotlinx.reflect.reflect
import kotlin.reflect.KClass


inline fun afActive(self: Active, id: String, init: AFActiveDefault.Builder.() -> Unit = {}): AFActive {
    val builder = AFActiveDefault.Builder(self, id)
    builder.init()
    return builder.build()
}

open class AFActiveDefault(
        override val self: Active,
        afIdentity: String,
        val initialise: suspend () -> Unit,
        val execute: suspend () -> Unit,
        val finalise: suspend () -> Unit
) : AFPassiveDefault(self, afIdentity), AFActive {

    class Builder(val self: Active, val id: String) {
        var initialise: suspend () -> Unit = {}
        var execute: suspend () -> Unit = {}
        var finalise: suspend () -> Unit = {}
        fun build(): AFActive {
            return AFActiveDefault(self, id, initialise, execute, finalise)
        }
    }

    override suspend fun start() {
        log.trace { "start" }
        log.trace { "initialise" }
        this.initialise()

        val activeParts = super.framework.partsOf(self).filterIsInstance<Active>()
        activeParts.forEach {
            it.af.start()
        }

        log.trace { "execute" }
        this.execute()

        activeParts.forEach {
            it.af.join()
        }
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
       //     it.af.join()
        }
        this.finalise()
    }

    override suspend fun terminate() {
        log.trace { "terminate" }
        val activeParts = super.framework.partsOf(self).filterIsInstance<Active>()
        activeParts.forEach {
            it.af.terminate()
        }
    }

    override fun <T : Any> receiver(forInterface:KClass<*>): T {
        return super.framework.receiver(forInterface) { proxy, callable, args ->
            when {
                forInterface.isInstance(self) -> self.reflect().call(callable.name, *args)
                else -> throw ActiveException("${self.af.identity}:${self::class.simpleName!!} does not implement ${forInterface.simpleName!!}")
            }
        }
    }
}