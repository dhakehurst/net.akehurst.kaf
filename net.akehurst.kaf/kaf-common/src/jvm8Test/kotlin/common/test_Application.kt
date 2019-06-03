package net.akehurst.kaf.common

import net.akehurst.kaf.api.Active
import net.akehurst.kaf.api.Application
import net.akehurst.kaf.service.logging.api.LogLevel
import net.akehurst.kaf.service.logging.api.Logger
import net.akehurst.kaf.service.logging.console.LoggerConsole
import kotlin.test.Test


class test_Application {

    class TestApplication(id:String) : Application {

        @Composite
        val comp = object : Active {
            override  val af = afActive(this, "comp") {
                execute = {
                    self.logger.log(LogLevel.INFO, {"I have started"})
                }
            }
        }

        override val af = afApplication(this, id) {
            services["logger"] = LoggerConsole(LogLevel.ALL)
            execute = {
                comp.af.start()
            }
        }
    }


    @Test
    fun test() {

        val sut = TestApplication("sut")
        sut.af.start("")

    }

}
