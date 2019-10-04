package net.akehurst.kaf.service.logging.api

import net.akehurst.kaf.service.api.Service
import net.akehurst.kaf.service.api.ServiceReference
import kotlin.reflect.KProperty

class LoggingServiceException : RuntimeException {
    constructor(message:String) : super(message)
}

fun logger(serviceIdentity: String): LoggerReference {
    return LoggerReference(serviceIdentity)
}

class LoggerReference(
        serviceIdentity: String
) : ServiceReference<Logger>(serviceIdentity) {

    override fun setValue(owner: Any, ownerIdentity:String, property: KProperty<*>, value: Any) {
        val v = (value as LoggingService).create(ownerIdentity)
        super.setValue(owner, ownerIdentity, property, v)
    }
}

interface LoggingService : Service {
    fun create(identity: String): Logger
}

interface Logger : Service {
    fun fatal(message: () -> String?) = log(LogLevel.FATAL, message)
    fun error(message: () -> String?) = log(LogLevel.ERROR, message)
    fun warn(message: () -> String?) = log(LogLevel.WARN, message)
    fun info(message: () -> String?) = log(LogLevel.INFO, message)
    fun debug(message: () -> String?) = log(LogLevel.DEBUG, message)
    fun trace(message: () -> String?) = log(LogLevel.TRACE, message)

    fun log(level: LogLevel, message: () -> String?)
}

class LogLevel(val value: Int, val stringValue: String) {

    companion object {
        val OFF = LogLevel(10000, "OFF")
        val FATAL = LogLevel(6000, "FATAL")
        val ERROR = LogLevel(5000, "ERROR")
        val WARN = LogLevel(4000, "WARN")
        val INFO = LogLevel(3000, "INFO")
        val DEBUG = LogLevel(2000, "DEBUG")
        val TRACE = LogLevel(1000, "TRACE")
        val ALL = LogLevel(0, "ALL")
    }

    override fun hashCode(): Int {
        return this.value
    }

    override fun equals(other: Any?): Boolean {
        return when {
            !(other is LogLevel) -> false
            else -> this.value == other.value
        }
    }

    override fun toString(): String {
        return this.stringValue
    }
}