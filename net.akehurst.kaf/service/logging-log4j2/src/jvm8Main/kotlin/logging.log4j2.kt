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

package net.akehurst.kaf.service.logging.log4j2

import net.akehurst.kaf.service.logging.api.LogLevel
import net.akehurst.kaf.service.logging.api.Logger
import net.akehurst.kaf.service.logging.api.LoggingService
import net.akehurst.kaf.service.logging.api.LoggingServiceException

class LoggingServiceLog4j2(
) : LoggingService {

    override fun create(identity: String): Logger {
        return LoggerLog4j2(org.apache.logging.log4j.LogManager.getLogger(identity))
    }

}

internal class LoggerLog4j2(
        val impl: org.apache.logging.log4j.Logger
) : Logger {

    companion object {
        fun convertLevel(level: LogLevel): org.apache.logging.log4j.Level {
            return when (level) {
                LogLevel.OFF -> org.apache.logging.log4j.Level.OFF
                LogLevel.FATAL -> org.apache.logging.log4j.Level.FATAL
                LogLevel.ERROR -> org.apache.logging.log4j.Level.ERROR
                LogLevel.WARN -> org.apache.logging.log4j.Level.WARN
                LogLevel.INFO -> org.apache.logging.log4j.Level.INFO
                LogLevel.DEBUG -> org.apache.logging.log4j.Level.DEBUG
                LogLevel.TRACE -> org.apache.logging.log4j.Level.TRACE
                LogLevel.ALL -> org.apache.logging.log4j.Level.ALL
                else -> {
                    org.apache.logging.log4j.LogManager.getRootLogger().error("Unknown log level $level")
                    org.apache.logging.log4j.Level.ALL
                }
            }
        }
    }

    override fun log(level: LogLevel, throwable: Throwable?, message: () -> String?) {
        val log4jLevel = convertLevel(level)
        if (null == throwable) {
            this.impl.log(log4jLevel) { org.apache.logging.log4j.message.SimpleMessage(message.invoke()) }
        } else {
            this.impl.log(log4jLevel, { org.apache.logging.log4j.message.SimpleMessage(message.invoke()) }, throwable)
        }
    }

}