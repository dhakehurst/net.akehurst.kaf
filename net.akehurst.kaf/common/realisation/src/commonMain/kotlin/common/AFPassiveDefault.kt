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
        selfIdentity: String? = null,
        open val initialiseBlock: suspend (self: Passive) -> Unit
) : AFDefault(selfIdentity), AFPassive {

    class Builder(val selfIdentity: String?) {
        var initialise: suspend (self: Passive) -> Unit = {}
        fun build(): AFPassive {
            return AFPassiveDefault(selfIdentity, initialise)
        }
    }

    override val self : Passive get() = super.self()

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
}

