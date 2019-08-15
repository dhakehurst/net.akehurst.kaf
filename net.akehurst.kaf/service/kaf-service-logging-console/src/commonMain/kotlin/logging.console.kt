package net.akehurst.kaf.service.logging.console

import net.akehurst.kaf.service.logging.api.*

class LoggingServiceConsole(
        initLevel: LogLevel = LogLevel.INFO
) : LoggingService {

    var outputLevel = initLevel

    override fun create(identity: String): Logger {
        return LoggerConsole(this, identity)
    }

}

internal class LoggerConsole(
        val service: LoggingServiceConsole,
        val identity: String
) : Logger {

    override fun log(level: LogLevel, message: () -> String?) {
        if (level.value >= this.service.outputLevel.value) {
            val out = "[$level]: ${message()}"
            println(out)
        }
    }

}