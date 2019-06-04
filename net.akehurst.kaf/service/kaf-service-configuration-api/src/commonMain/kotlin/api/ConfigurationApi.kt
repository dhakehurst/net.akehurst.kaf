package net.akehurst.kaf.service.configuration.api

import net.akehurst.kaf.service.api.Reference
import net.akehurst.kaf.service.api.Service
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

interface Configuration : Service {
    fun <T> get(path:String, default:()->T):T
}

inline fun <T : Any> configuredValue(configurationServiceName:String, path: String, noinline default:()->T): ConfiguredValue<T> {
    return ConfiguredValue<T>(configurationServiceName, path, default)
}


class ConfiguredValue<T : Any>(
        val configurationServiceName:String,
        val path:String,
        val default: ()->T
) : ReadOnlyProperty<Any, T> {

    lateinit var configuration: Configuration

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        return this.configuration.get(this.path, this.default)
    }
}