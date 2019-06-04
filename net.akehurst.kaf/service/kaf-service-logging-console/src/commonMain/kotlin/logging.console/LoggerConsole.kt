package net.akehurst.kaf.service.logging.console

import net.akehurst.kaf.service.logging.api.*

class LoggerConsole(
        initLevel: LogLevel = LogLevel.INFO
) : Logger {

    var outputLevel = initLevel

    override fun log(level: LogLevel, message: () -> String?) {
        if (level.value >= this.outputLevel.value) {
            val out = "[$level]: ${message()}"
            println(out)
        }
    }

}