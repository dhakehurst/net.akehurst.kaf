package net.akehurst.kaf.service.api

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

interface Service {

}

open class Reference<T : Any>(val reference: String) : ReadWriteProperty<Any, T> {
    lateinit var value: T
    override operator fun getValue(thisRef: Any, property: KProperty<*>): T {
        return this.value
    }

    override operator fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        this.value = value
    }
}

class ServiceReference<T : Any>(reference: String) : Reference<T>(reference)

fun <T : Any> serviceReference(serviceIdentity: String): ServiceReference<T> {
    return ServiceReference<T>(serviceIdentity)
}