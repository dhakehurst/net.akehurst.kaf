package net.akehurst.kaf.service.api

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

interface Service {

}

open class Reference<T : Any>(val reference: String) : ReadOnlyProperty<Any, T> {

    private lateinit var _value: T

    override operator fun getValue(thisRef: Any, property: KProperty<*>): T {
        return this._value
    }

    open fun setValue(owner: Any, ownerIdentity:String, property: KProperty<*>, value: Any) {
        this._value = value as T
    }
}

open class ServiceReference<T : Any>(reference: String) : Reference<T>(reference) {
}

fun <T : Any> serviceReference(serviceIdentity: String): ServiceReference<T> {
    return ServiceReference<T>(serviceIdentity)
}