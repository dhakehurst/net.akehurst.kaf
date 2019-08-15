package net.akehurst.kaf.service.logging.console

import net.akehurst.kaf.service.logging.api.LogLevel
import kotlin.test.Test

class test_LoggerConsole {

    @Test
    fun test() {

        val ls = LoggingServiceConsole(LogLevel.ALL)
        val sut = ls.create("sut")

        sut.log(LogLevel.FATAL, {"Fatal Message"})
        sut.log(LogLevel.ERROR, {"Error Message"})
        sut.log(LogLevel.WARN, {"Warn Message"})
        sut.log(LogLevel.INFO, {"Info Message"})
        sut.log(LogLevel.DEBUG, {"Debug Message"})
        sut.log(LogLevel.TRACE, {"Trace Message"})

    }

    @Test
    fun test2() {

        val ls = LoggingServiceConsole(LogLevel.INFO)
        val sut = ls.create("sut")

        sut.log(LogLevel.FATAL, {"Fatal Message"})
        sut.log(LogLevel.ERROR, {"Error Message"})
        sut.log(LogLevel.WARN, {"Warn Message"})
        sut.log(LogLevel.INFO, {"Info Message"})
        sut.log(LogLevel.DEBUG, {"Debug Message"})
        sut.log(LogLevel.TRACE, {"Trace Message"})

    }

}