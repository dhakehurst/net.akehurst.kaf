package net.akehurst.kaf.common

import net.akehurst.kaf.api.*
import kotlin.reflect.KProperty
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubtypeOf


class ApplicationCompositionWalker {

     fun walkDepthFirst(self: Any, func:(obj:Any, property:KProperty<*>)->Unit) {
        when (self) {
            is Application -> this._walkDepthFirst(self, func)
            is Component -> this._walkDepthFirst(self, func)
            is Active -> this._walkDepthFirst(self, func)
            is Identifiable -> this._walkDepthFirst(self, func)
            is AFIdentifiable -> this._walkDepthFirst(self, func)
        }
    }

    private fun _walkDepthFirst(self:Any, func:(obj:Any, property:KProperty<*>)->Unit) {
        self::class.members.filter { it is KProperty<*> }.forEach { property ->
            if (property is KProperty<*>) {
                val annotation = property.findAnnotation<CompositePart>()
                if (null!=annotation || property.returnType.isSubtypeOf(AFIdentifiable::class.createType())) {
                    val composite = property.getter.call(self)
                    if (null!=composite) {
                        this.walkDepthFirst(composite, func)
                    }
                }
                func(self, property)
            }
        }
    }

}

