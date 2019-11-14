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


inline fun afPassive(selfIdentity: String? = null, init: AFPassiveDefault.Builder.() -> Unit = {}): AFPassive {
    val builder = AFPassiveDefault.Builder(selfIdentity)
    builder.init()
    return builder.build()
}

open class AFPassiveDefault(
        override var selfIdentity: String? = null
) : AFPassive {

    class Builder(val selfIdentity: String?) {
        fun build(): AFPassive {
            return AFPassiveDefault(selfIdentity)
        }
    }

    override var afHolder: AFHolder? = null
    override val self: Passive
        get() {
            return when (afHolder) {
                is Passive -> afHolder as Passive? ?: throw ApplicationInstantiationException("afHolder has not been set to a value")
                else -> throw ApplicationInstantiationException("afHolder must be of type Passive for $identity")
            }
        }
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

    override val framework by serviceReference<ApplicationFrameworkService>()

    override val log by logger("logging")

    override fun externalConnections(kclass: KClass<*>): Map<KProperty<*>, ExternalConnection<*>> = framework.externalConnections(self, kclass)

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

