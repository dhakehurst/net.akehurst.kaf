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

import net.akehurst.kaf.common.api.AFPassive
import net.akehurst.kaf.common.api.ApplicationFrameworkService
import net.akehurst.kaf.common.api.ExternalConnection
import net.akehurst.kaf.common.api.Passive
import net.akehurst.kaf.service.api.serviceReference
import net.akehurst.kaf.service.logging.api.logger
import kotlin.reflect.KClass
import kotlin.reflect.KProperty


inline fun afPassive(self: Passive, id: String, init: AFPassiveDefault.Builder.() -> Unit = {}): AFPassive {
    val builder = AFPassiveDefault.Builder(self, id)
    builder.init()
    return builder.build()
}

open class AFPassiveDefault(
        override val self: Passive,
        override val identity: String
) : AFPassive {

    class Builder(val self: Passive, val identity: String) {
        fun build(): AFPassive {
            return AFPassiveDefault(self, identity)
        }
    }

    val framework by serviceReference<ApplicationFrameworkService>()
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

