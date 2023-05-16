package net.akehurst.kaf.engineering.channel.genericMessageChannel.test

import net.akehurst.kaf.common.api.Application
import net.akehurst.kaf.common.realisation.afApplication
import net.akehurst.kaf.engineering.channel.genericMessageChannel.test.engineering.Gui2User
import net.akehurst.kaf.engineering.channel.genericMessageChannel.test.engineering.User2Gui
import net.akehurst.kaf.engineering.channel.genericMessageChannel.test.computational.Core
import net.akehurst.kaf.engineering.channel.genericMessageChannel.test.computational.Gui
import net.akehurst.kaf.engineering.channel.genericMessageChannel.test.engineering.Serialiser
import net.akehurst.kaf.engineering.genericMessageChannel.TestCredentials
import net.akehurst.kaf.service.commandLineHandler.api.CommandLineHandlerService
import net.akehurst.kaf.service.commandLineHandler.simple.CommandLineHandlerSimple
import net.akehurst.kaf.service.configuration.api.ConfigurationService
import net.akehurst.kaf.service.configuration.map.ServiceConfigurationMap
import net.akehurst.kaf.service.logging.api.LogLevel
import net.akehurst.kaf.service.logging.api.LoggingService
import net.akehurst.kaf.service.logging.console.LoggingServiceConsole
import net.akehurst.kaf.technology.messageChannel.inMemory.component_MessageChannelInMemory
import kotlin.test.Test

class test_channel {

    class TestApplication(afId: String) : Application {
        // computational
        val core = Core()
        val gui = Gui()

        // engineering
        val user2Gui = User2Gui()
        val gui2User = Gui2User()

        //  technology
        val comms = component_MessageChannelInMemory<String>()

        override val af = afApplication(this, afId) {
            defineService(ConfigurationService::class) {
                ServiceConfigurationMap(
                    mutableMapOf(
                        "sut.greeter.greeting" to "Hello World!"
                    )
                )
            }
            defineService(LoggingService::class) { LoggingServiceConsole(LogLevel.ALL) }
            defineService(CommandLineHandlerService::class) { commandLineArgs -> CommandLineHandlerSimple(commandLineArgs) }
            initialise = {
                core.port_user.connectPort(user2Gui.port_user)
                gui.port_core.connectPort(gui2User.port_gui)

                comms.port_endPoint1.connectPort(user2Gui.port_comms)
                comms.port_endPoint2.connectPort(gui2User.port_comms)

            }

        }

    }

    @Test
    fun test_application() {
        kaf_engineering_channel_genericMessageChannel.KotlinxReflectForModule.registerUsedClasses()
        val sut = TestApplication("sut")
        sut.af.startBlocking(emptyList())
    }

    @Test
    fun serialiser() {
       kaf_engineering_channel_genericMessageChannel.KotlinxReflectForModule.registerUsedClasses()

        val data = TestCredentials("testUser", "testPwd")
        val sut = Serialiser()
        val json = sut.toJson(data, data)

        val data2 = sut.toData(json.toStringJson()) as TestCredentials

    }

}