package net.akehurst.kaf.common

import net.akehurst.kaf.api.AFApplication
import net.akehurst.kaf.api.Application
import net.akehurst.kaf.api.Port
import net.akehurst.kaf.service.api.Service

actual inline fun afApplication(self: Application, id: String, init: AFApplicationDefault.Builder.() -> Unit): AFApplication {
    val builder = AFApplicationDefault.Builder(self, id)
    builder.init()
    return builder.build()
}

actual class AFApplicationDefault(
        val self: Application,
        afIdentity: String,
        val defineServices: (commandLineArgs: List<String>) -> Map<String, Service>,
        initialise: () -> Unit,
        execute: () -> Unit,
        terminate: () -> Unit
) : AFActiveDefault(afIdentity, initialise, execute, terminate), AFApplication {

    actual class Builder(val self: Application, val id: String) {
        actual var initialise: () -> Unit = {}
        actual var execute: () -> Unit = {}
        actual var terminate: () -> Unit = {}
        actual var defineServices: (commandLineArgs: List<String>) -> Map<String, Service> = { emptyMap() }
        actual fun build(): AFApplication {
            return AFApplicationDefault(self, id, defineServices, initialise, execute, terminate)
        }
    }

    private val _services = mutableMapOf<String, Service>()
    val services: Map<String, Service>
        get() {
            return this._services
        }

    override val ports: Set<Port>
        get() = TODO("not implemented")


    override fun start(commandLineArgs: List<String>) {
        this._services.putAll(this.defineServices(commandLineArgs))
        val fws = ApplicationFrameworkService("${this.identity}.framework", this._services)
        this._services["framework"] = fws
        fws.doInjections(commandLineArgs, this.self)
        super.log.trace { "[${this.identity}] Application.start" }
        super.start()
    }

}