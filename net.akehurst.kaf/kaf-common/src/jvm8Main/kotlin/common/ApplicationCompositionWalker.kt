package net.akehurst.kaf.common

import net.akehurst.kaf.api.*
import net.akehurst.kaf.service.api.Service
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType


class ApplicationCompositionWalker {

    companion object {
        val COMPOSITE_PARTS_APPLICATION = listOf(Service::class.starProjectedType, Component::class.starProjectedType, Active::class.starProjectedType, Identifiable::class.starProjectedType)
        val COMPOSITE_PARTS_SERVICE = listOf(Service::class.starProjectedType, Identifiable::class.starProjectedType)
        val COMPOSITE_PARTS_COMPONENT = listOf(Service::class.starProjectedType, Component::class.starProjectedType, Active::class.starProjectedType, Identifiable::class.starProjectedType)
        val COMPOSITE_PARTS_ACTIVE = listOf(Service::class.starProjectedType, Active::class.starProjectedType, Identifiable::class.starProjectedType)
        val COMPOSITE_PARTS_IDENTIFIABLE = emptyList<KType>()
        val COMPOSITE_PARTS_AF = listOf(Service::class.starProjectedType)
    }

     fun walkDepthFirst(self: Identifiable, func:(obj:Identifiable, property:KProperty<*>)->Unit) {
        when (self) {
            is Application -> this._walkDepthFirst(self, func, COMPOSITE_PARTS_APPLICATION)
            is Service -> this._walkDepthFirst(self, func, COMPOSITE_PARTS_SERVICE)
            is Component -> this._walkDepthFirst(self, func, COMPOSITE_PARTS_COMPONENT)
            is Active -> this._walkDepthFirst(self, func, COMPOSITE_PARTS_ACTIVE)
            is Identifiable -> this._walkDepthFirst(self, func, COMPOSITE_PARTS_IDENTIFIABLE)
        }
    }

    private fun _walkDepthFirst(self:Identifiable, func:(obj:Identifiable, property:KProperty<*>)->Unit, compositeTypes:List<KType>) {
        self::class.members.filter { it is KProperty<*> }.forEach { property ->
            if (property is KProperty<*>) {
                func(self, property)

                if(compositeTypes.any { property.returnType.isSubtypeOf(it) }) {
                    val composite = property.getter.call(self)
                    if (null!=composite && composite is Identifiable) {
                        this.walkDepthFirst(composite, func)
                    }
                }

                // walk things marked as 'CompositePart'
                val annotation = property.findAnnotation<CompositePart>()
                if (null!=annotation) {
                    val composite = property.getter.call(self)
                    if (null!=composite && composite is Identifiable) {
                        this.walkDepthFirst(composite, func)
                    }
                }

            }
        }
    }


}

