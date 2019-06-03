package net.akehurst.kaf.common

import net.akehurst.kaf.api.Application
import net.akehurst.kaf.api.Service
import net.akehurst.kaf.service.logging.api.LogLevel
import net.akehurst.kaf.service.logging.api.Logger
import kotlin.reflect.KProperty
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf


actual inline fun afApplication(self: Application, id: String, init: AFApplication.Builder.() -> Unit): AFApplication {
    val builder = AFApplication.Builder(self, id)
    builder.init()
    return builder.build()
}

actual class AFApplication(
        val self: Application,
        afIdentity: String,
        val services: Map<String, Service>,
        initialise: () -> Unit,
        execute: () -> Unit,
        terminate: () -> Unit
) : AFActiveDefault(afIdentity, initialise, execute, terminate) {

    actual class Builder(val self: Application, val id: String) {
        actual var initialise: () -> Unit = {}
        actual var execute: () -> Unit = {}
        actual var terminate: () -> Unit = {}
        actual val services = mutableMapOf<String, Service>()
        actual fun build(): AFApplication {
            return AFApplication(self, id, services, initialise, execute, terminate)
        }
    }

    fun start(vararg commandLineArgs: String) {
        logger().log(LogLevel.DEBUG, {"[${this.identity}] start"})
        this.doInjections()
        super.start()
    }

    private fun logger() : Logger {
        return this.services["logger"] as Logger
    }

    private fun doInjections() {
        this.injectServiceReferences();

        this.defineCommandLine();
        // this.commandLineHandler.parse();

        this.injectConfigurationValues();
        this.injectCommandLineArgs();
    }


    private fun injectServiceReferences() {
        logger().log(LogLevel.DEBUG, {"[${this.identity}] injectServiceReferences"})
        val walker = ApplicationCompositionWalker()
        walker.walkDepthFirst(this.self) { obj, property ->
            when {
                property.returnType.isSubtypeOf(Service::class.createType()) -> {
                    val ref = property.getter.call(obj) as ServiceReference<*>
                    logger().log(LogLevel.TRACE, {"${this.identity}.${property.name} = service[${ref.reference}]"})
                    (ref as ServiceReference<Service>).value = this.services[ref.reference] ?: throw ServiceNotFoundException(ref.reference)
                }
            }
        }
    }

    private fun injectServiceReference(obj:Any, property: KProperty<*>) {

    }

    private fun defineCommandLine() {

    }

    private fun injectConfigurationValues() {

    }

    private fun injectCommandLineArgs() {

    }
}