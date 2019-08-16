package net.akehurst.kaf.service.configuration.api

import net.akehurst.kaf.api.Identifiable
import net.akehurst.kaf.service.api.Reference
import net.akehurst.kaf.service.api.Service
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

interface Configuration : Service {
    fun <T> get(path:String, default:()->T):T
}

fun <T : Any> configuredValue(configurationServiceName:String, default:()->T): ConfiguredValue<T> {
    return ConfiguredValue<T>(configurationServiceName, default)
}


class ConfiguredValue<T : Any>(
        val configurationServiceName:String,
        val default: ()->T
) : ReadOnlyProperty<Identifiable, T> {

    lateinit var configuration: Configuration

    override fun getValue(thisRef: Identifiable, property: KProperty<*>): T {
        val path = "${thisRef.af.identity}.${property.name}"
        return this.configuration.get(path, this.default)
    }
}