package net.akehurst.kaf.common

import net.akehurst.kaf.api.AFApplication
import net.akehurst.kaf.api.Application
import net.akehurst.kaf.service.api.Service
import net.akehurst.kaf.service.api.ServiceReference
import net.akehurst.kaf.service.logging.api.LogLevel
import net.akehurst.kaf.service.logging.api.Logger
import kotlin.reflect.KProperty1

actual inline fun afApplication(self: Application, id: String, init: AFApplicationDefault.Builder.() -> Unit): AFApplication {
    val builder = AFApplicationDefault.Builder(self, id)
    builder.init()
    return builder.build()
}

actual class AFApplicationDefault(
        val self: Application,
        afIdentity: String,
        val defineServices: (commandLineArgs:List<String>) -> Map<String, Service>,
        initialise: () -> Unit,
        execute: () -> Unit,
        terminate: () -> Unit
) : AFComponentDefault(afIdentity, initialise, execute, terminate), AFApplication {

    actual class Builder(
            val self: Application,
            val id: String
    ) {
        actual var initialise: () -> Unit = {}
        actual var execute: () -> Unit = {}
        actual var terminate: () -> Unit = {}
        actual var defineServices: (commandLineArgs:List<String>) -> Map<String, Service> = { emptyMap() }

        actual fun build(): AFApplication {
            return AFApplicationDefault(self, id, defineServices, initialise, execute, terminate)
        }
    }

    private lateinit var _services:Map<String, Service>
    val services:Map<String, Service> get() { return this._services }

    private fun logger(): Logger {
        return this.services["logger"] as Logger
    }

    private fun doInjections(commandLineArgs: List<String>) {
        //TODO: native can't support reflection!
    }



    override fun start(commandLineArgs: List<String>) {
        this._services = this.defineServices(commandLineArgs)
        logger().log(LogLevel.DEBUG, { "[${this.identity}] Application.start" })
        this.doInjections(commandLineArgs)
        super.start()
    }
}