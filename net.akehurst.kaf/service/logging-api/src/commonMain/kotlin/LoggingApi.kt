/**
 * Copyright (C) 2019 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
) : ServiceReference<Logger>(LoggingService::class) {

    override fun setValue(owner: Any, ownerIdentity:String, property: KProperty<*>, value: Any) {
        val v = (value as LoggingService).create(ownerIdentity)
        super.setValue(owner, ownerIdentity, property, v)
    }
}

interface LoggingService : Service {
    fun create(identity: String): Logger
}

interface Logger : Service {
    fun fatal(throwable:Throwable? = null, message: () -> String?) = log(LogLevel.FATAL, throwable, message)
    fun error(throwable:Throwable? = null, message: () -> String?) = log(LogLevel.ERROR, throwable, message)
    fun warn(throwable:Throwable? = null, message: () -> String?) = log(LogLevel.WARN, throwable, message)
    fun info(throwable:Throwable? = null, message: () -> String?) = log(LogLevel.INFO, throwable, message)
    fun debug(throwable:Throwable? = null, message: () -> String?) = log(LogLevel.DEBUG, throwable, message)
    fun trace(throwable:Throwable? = null, message: () -> String?) = log(LogLevel.TRACE, throwable, message)

    fun log(level: LogLevel, message: () -> String?) = log(level, null, message)
    fun log(level: LogLevel, throwable:Throwable? = null, message: () -> String?)
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