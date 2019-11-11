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
import net.akehurst.kaf.service.api.Service
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.KVisibility
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType


class ApplicationCompositionWalker {

    companion object {
        val COMPOSITE_PARTS_APPLICATION = listOf(Service::class.starProjectedType, Component::class.starProjectedType, Actor::class.starProjectedType, Active::class.starProjectedType, Passive::class.starProjectedType)
        val COMPOSITE_PARTS_SERVICE = listOf(Service::class.starProjectedType, Passive::class.starProjectedType)
        val COMPOSITE_PARTS_COMPONENT = listOf(Service::class.starProjectedType, Component::class.starProjectedType, Actor::class.starProjectedType, Active::class.starProjectedType, Passive::class.starProjectedType)
        val COMPOSITE_PARTS_ACTIVE = listOf(Service::class.starProjectedType, Actor::class.starProjectedType, Active::class.starProjectedType, Passive::class.starProjectedType)
        val COMPOSITE_PARTS_PASSIVE = emptyList<KType>()
        val COMPOSITE_PARTS_AF = listOf(Service::class.starProjectedType)
    }

    fun walkDepthFirst(self: AFHolder, selfFunc: (obj: AFHolder) -> Unit, propFunc: (obj: AFHolder, property: KProperty<*>) -> Unit) {
        when (self) {
            is Application -> this._walkDepthFirst(self, selfFunc, propFunc, COMPOSITE_PARTS_APPLICATION)
            is Service -> this._walkDepthFirst(self, selfFunc, propFunc, COMPOSITE_PARTS_SERVICE)
            is Component -> this._walkDepthFirst(self, selfFunc, propFunc, COMPOSITE_PARTS_COMPONENT)
            is Active -> this._walkDepthFirst(self, selfFunc, propFunc, COMPOSITE_PARTS_ACTIVE)
            is Passive -> this._walkDepthFirst(self, selfFunc, propFunc, COMPOSITE_PARTS_PASSIVE)
        }
        selfFunc(self)
    }

    private fun _walkDepthFirst(self: AFHolder, selfFunc: (obj: AFHolder) -> Unit, propFunc: (obj: AFHolder, property: KProperty<*>) -> Unit, compositeTypes: List<KType>) {
        self::class.members.filter { it is KProperty<*> }.forEach { property ->
            if (property is KProperty<*>) {
                propFunc(self, property)

                if (compositeTypes.any { property.returnType.isSubtypeOf(it) }) {
                    if (property.visibility!=KVisibility.PRIVATE) {
                        val part = property.getter.call(self)
                        if (null != part && part is Passive) {
                            this.walkDepthFirst(part, selfFunc, propFunc)
                        }
                    } else {
                        //TODO: warning! maybe
                       // throw ApplicationInstantiationException("Cannot walk private property $property")
                    }
                }

                // walk things marked as 'CompositePart'
                val annotation = property.findAnnotation<CompositePart>()
                if (null != annotation) {
                    val part = property.getter.call(self)
                    if (null != part && part is Passive) {
                        this.walkDepthFirst(part, selfFunc, propFunc)
                    }
                }

            }
        }
    }

    fun walkAfParts(self: Owner, partFunc: (part: Passive, property: KProperty<*>) -> Unit) {
        when (self) {
            is Application -> this._walkAfParts(self, partFunc, COMPOSITE_PARTS_APPLICATION)
            is Service -> this._walkAfParts(self, partFunc, COMPOSITE_PARTS_SERVICE)
            is Component -> this._walkAfParts(self, partFunc, COMPOSITE_PARTS_COMPONENT)
            is Active -> this._walkAfParts(self, partFunc, COMPOSITE_PARTS_ACTIVE)
            is Passive -> this._walkAfParts(self, partFunc, COMPOSITE_PARTS_PASSIVE)
        }
    }

    private fun _walkAfParts(self: Owner, partFunc: (part: Passive, property: KProperty<*>) -> Unit, compositeTypes: List<KType>) {
        self::class.members.filter { it is KProperty<*> }.forEach { property ->
            if (property is KProperty<*>) {
                if (compositeTypes.any { property.returnType.isSubtypeOf(it) }) {
                    val part = property.getter.call(self)
                    if (null != part && part is Passive) {
                        partFunc(part, property)
                    }
                }

                // walk things marked as 'CompositePart'
                val annotation = property.findAnnotation<CompositePart>()
                if (null != annotation) {
                    val part = property.getter.call(self)
                    if (null != part && part is Passive) {
                        partFunc(part, property)
                    }
                }

            }
        }
    }
}

