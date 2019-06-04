package net.akehurst.kaf.service.logging.api

import net.akehurst.kaf.service.api.Service

interface Logger : Service {
    fun log(level: LogLevel, message: () -> String?)
}

class LogLevel(val value: Int, val stringValue: String) {

    companion object {
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