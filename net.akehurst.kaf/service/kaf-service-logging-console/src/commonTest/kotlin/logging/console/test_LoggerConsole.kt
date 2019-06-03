package net.akehurst.kaf.service.logging.console

import net.akehurst.kaf.service.logging.api.LogLevel
import kotlin.test.Test

class test_LoggerConsole {

    @Test
    fun test() {

        val sut = LoggerConsole()

        sut.log(LogLevel.FATAL, {"Fatal Message"})
        sut.log(LogLevel.ERROR, {"Error Message"})
        sut.log(LogLevel.WARN, {"Warn Message"})
        sut.log(LogLevel.INFO, {"Info Message"})
        sut.log(LogLevel.DEBUG, {"Debug Message"})
        sut.log(LogLevel.TRACE, {"Trace Message"})

    }

    @Test
    fun test2() {

        val sut = LoggerConsole(LogLevel.FATAL)

        sut.log(LogLevel.FATAL, {"Fatal Message"})
        sut.log(LogLevel.ERROR, {"Error Message"})
        sut.log(LogLevel.WARN, {"Warn Message"})
        sut.log(LogLevel.INFO, {"Info Message"})
        sut.log(LogLevel.DEBUG, {"Debug Message"})
        sut.log(LogLevel.TRACE, {"Trace Message"})

    }

}