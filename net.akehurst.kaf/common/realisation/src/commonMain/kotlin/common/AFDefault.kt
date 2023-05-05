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
import net.akehurst.kaf.service.api.serviceReference
import net.akehurst.kaf.service.logging.api.logger
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

abstract class AFDefault(
        override var selfIdentity: String? = null
) : AF {

    override var afHolder: AFHolder? = null

    abstract override val identity: String

    override val framework by serviceReference<ApplicationFrameworkService>()

    override val log by logger("logging")

    protected inline fun <reified T : AFHolder> self(): T {
        return when (afHolder) {
            is T -> afHolder as T? ?: throw ApplicationInstantiationException("afHolder has not been set to a value")
            else -> throw ApplicationInstantiationException("afHolder must be of type ${T::class} for $identity")
        }
    }

    override fun externalConnections(klass: KClass<*>): Map<KProperty<*>, ExternalConnection<*>> = framework.externalConnections(self(), klass)

    override fun doInjections(root: Passive) {
        framework.doInjections(emptyList<String>(), root)
    }

    override fun hashCode(): Int {
        return this.identity.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is AFPassive -> this.identity == other.identity
            else -> false
        }
    }

    override fun toString(): String {
        return "AF{$identity}"
    }
}

