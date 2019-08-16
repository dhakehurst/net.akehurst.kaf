package net.akehurst.kaf.service.commandLineHandler.api

import net.akehurst.kaf.api.Identifiable
import net.akehurst.kaf.service.api.Service
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

interface CommandLineHandler : Service {
    fun <T : Any?> get(path: String, default: () -> T?): T?
    fun <T : Any> registerOption(path: String, type: KClass<T>, default: T?, description: String, hidden: Boolean)
}

inline fun <reified T : Any> commandLineValue(handlerServiceName: String, noinline default: () -> T?): CommandLineValue<T> {
    return CommandLineValue<T>(handlerServiceName, T::class, default)
}


class CommandLineValue<T : Any>(
        val handlerServiceName: String,
        val type: KClass<T>,
        val default: () -> T?,
        val description: String = ""
) : ReadOnlyProperty<Identifiable, T?> {

    lateinit var path: String
    lateinit var handler: CommandLineHandler

    override fun getValue(thisRef: Identifiable, property: KProperty<*>): T? {
        return this.handler.get(this.path, this.default)
    }

    fun setHandlerAndRegister(path:String, handler: CommandLineHandler) {
        this.path = path
        this.handler = handler
        handler.registerOption(this.path, type, this.default(), this.description, false)
    }
}