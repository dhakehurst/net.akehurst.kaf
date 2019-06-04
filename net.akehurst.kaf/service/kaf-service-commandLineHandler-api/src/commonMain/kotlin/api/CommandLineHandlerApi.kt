package net.akehurst.kaf.service.commandLineHandler.api

import net.akehurst.kaf.service.api.Service
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

interface CommandLineHandler : Service {
    fun <T : Any?> get(path:String, default:()->T?):T?
}

inline fun <T : Any?> commandLineValue(handlerServiceName:String, path: String, noinline default:()->T?): CommandLineValue<T> {
    return CommandLineValue<T>(handlerServiceName, path, default)
}


class CommandLineValue<T : Any?>(
        val handlerServiceName:String,
        val path:String,
        val default:()->T?
) : ReadOnlyProperty<Any, T?> {

    lateinit var handler: CommandLineHandler

    override fun getValue(thisRef: Any, property: KProperty<*>): T? {
        return this.handler.get(this.path, this.default)
    }
}