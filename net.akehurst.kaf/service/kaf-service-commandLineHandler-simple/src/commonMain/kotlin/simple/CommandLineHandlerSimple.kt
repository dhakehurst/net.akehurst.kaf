package net.akehurst.kaf.service.commandLineHandler.simple

import net.akehurst.kaf.service.commandLineHandler.api.CommandLineHandler
import kotlin.reflect.KClass

class CommandLineHandlerSimple(
        commandLineArgs: List<String>
) : CommandLineHandler {

    val values:Map<String,Any> = commandLineArgs.mapNotNull {
        if (it.startsWith("--")) {
            val l = it.substringAfter("--").split("=")
            Pair(l[0], l[1])
        } else {
            null
        }
    }.associate { it }

    override fun <T> get(path: String, default:()->T?): T? {
        return this.values[path] as T ?: default()
    }

    override fun <T : Any> registerOption(path: String, type: KClass<T>, default: T?, description: String, hidden: Boolean) {
        //TODO:
    }
}