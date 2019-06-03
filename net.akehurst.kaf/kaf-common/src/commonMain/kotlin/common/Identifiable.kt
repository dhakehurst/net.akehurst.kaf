package net.akehurst.kaf.common

import net.akehurst.kaf.api.AFIdentifiable
import net.akehurst.kaf.service.logging.api.Logger
import kotlin.properties.ObservableProperty
import kotlin.reflect.KProperty

class ApplicationInstantiationException(sid:String) : Exception("Service with identity $sid is not found")
class ServiceNotFoundException(sid:String) : Exception("Service with identity $sid is not found")

open class Reference<T : Any>(val reference:String) {
    lateinit var value:T
    operator fun getValue(thisRef: Any?, property: KProperty<*>) : T {
        return this.value
    }
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value:T) {
        this.value = value
    }
}

class ServiceReference<T:Any>(reference:String) : Reference<T>(reference)

inline fun <T:Any> serviceReference(serviceIdentity:String):ServiceReference<T> {
    return ServiceReference<T>(serviceIdentity)
}

open class AFBase(
        val identity: String
)  : AFIdentifiable {

    val logger:Logger by serviceReference<Logger>("logger")

    override fun toString(): String {
        return "AF{$identity}"
    }
}

