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

    override fun log(level: LogLevel, message: () -> String?) {
        val log4jLevel = convertLevel(level)
        this.impl.log(log4jLevel) { org.apache.logging.log4j.message.SimpleMessage(message.invoke()) }
    }

}