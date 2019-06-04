package net.akehurst.kaf.common

import net.akehurst.kaf.api.Active
import net.akehurst.kaf.api.Application
import net.akehurst.kaf.api.Composite
import net.akehurst.kaf.service.api.serviceReference
import net.akehurst.kaf.service.commandLineHandler.api.commandLineValue
import net.akehurst.kaf.service.configuration.api.Configuration
import net.akehurst.kaf.service.configuration.api.configuredValue
import net.akehurst.kaf.service.configuration.map.ConfigurationMap
import net.akehurst.kaf.service.logging.api.LogLevel
import net.akehurst.kaf.service.logging.console.LoggerConsole
import kotlin.test.Test


class test_Application {

    class TestApplication(id:String) : Application {

        @Composite
        val comp = object : Active {
            val confGreeting:String by configuredValue("configuration", "greeting") { "unknown" }
            val greeting:String? by commandLineValue("cmdLineHandler", "greeting") { confGreeting }

            override  val af = afActive(this, "comp") {
                execute = {
                    self.af.logger.log(LogLevel.INFO, {greeting})
                }
            }
        }

        override val af = afApplication(this, id) {
            services["logger"] = LoggerConsole(LogLevel.ALL)
            services["configuration"] = ConfigurationMap(mutableMapOf(
                    "greeting" to "Hello World!"
            ))
            services["cmdLineHandler"] = C
            execute = {
                comp.af.start()
            }
        }
    }


    @Test
    fun test() {

        val sut = TestApplication("sut")
        sut.af.start(listOf("--greeting='Hello Jim'"))

    }

}
