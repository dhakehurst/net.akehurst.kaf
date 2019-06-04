package net.akehurst.kaf.common

import net.akehurst.kaf.api.Application
import net.akehurst.kaf.api.AFApplication
import net.akehurst.kaf.service.api.Service
import net.akehurst.kaf.service.api.ServiceReference
import net.akehurst.kaf.service.commandLineHandler.api.CommandLineHandler
import net.akehurst.kaf.service.commandLineHandler.api.CommandLineValue
import net.akehurst.kaf.service.configuration.api.Configuration
import net.akehurst.kaf.service.configuration.api.ConfiguredValue
import net.akehurst.kaf.service.logging.api.LogLevel
import net.akehurst.kaf.service.logging.api.Logger
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.isAccessible

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
) : AFActiveDefault(afIdentity, initialise, execute, terminate), AFApplication {

    actual class Builder(val self: Application, val id: String) {
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
        this.injectServiceReferences();

        this.defineCommandLine()//.process(commandLineArgs);

        this.injectConfigurationValues();
        this.injectCommandLineArgs();
    }


    private fun injectServiceReferences() {
        logger().log(LogLevel.DEBUG, { "[${this.identity}] injectServiceReferences" })
        val walker = ApplicationCompositionWalker()
        walker.walkDepthFirst(this.self) { obj, property ->
            when {
                property.returnType.isSubtypeOf(Service::class.starProjectedType) -> {
                    if (property is KProperty1<*, *>) {
                        property.isAccessible = true
                        val ref = (property as KProperty1<Any, Any>).getDelegate(obj) as ServiceReference<Service>
                        logger().log(LogLevel.TRACE, { "${this.identity}.${property.name} = service['${ref.reference}']" })
                        ref.value = this.services[ref.reference] ?: throw ServiceNotFoundException(ref.reference)
                    }
                }
            }
        }
    }

    private fun defineCommandLine() {

    }

    private fun injectConfigurationValues() {
        logger().log(LogLevel.DEBUG, { "[${this.identity}] injectConfigurationValues" })
        val walker = ApplicationCompositionWalker()
        walker.walkDepthFirst(this.self) { obj, property ->
            if (property is KProperty1<*, *>) {
                property.isAccessible = true
                if ((property as KProperty1<Any, Any>).getDelegate(obj) is ConfiguredValue<*>) {
                    val ref = (property as KProperty1<Any, Any>).getDelegate(obj) as ConfiguredValue<Any>
                    if (services.containsKey(ref.configurationServiceName) && services[ref.configurationServiceName] is Configuration) {
                        ref.configuration = services[ref.configurationServiceName] as Configuration
                        val value: Any = ref.configuration.get(ref.path, ref.default)
                        logger().log(LogLevel.TRACE, { "${this.identity}.${property.name} = ${value}" })
                    } else {
                        throw ApplicationInstantiationException("Cannot find a Configuration service named ${ref.configurationServiceName}, at ${this.identity}.${property.name}")
                    }
                }
            }
        }
    }

    private fun injectCommandLineArgs() {
        logger().log(LogLevel.DEBUG, { "[${this.identity}] injectCommandLineArgs" })
        val walker = ApplicationCompositionWalker()
        walker.walkDepthFirst(this.self) { obj, property ->
            if (property is KProperty1<*, *>) {
                property.isAccessible = true
                if ((property as KProperty1<Any, Any>).getDelegate(obj) is CommandLineValue<*>) {
                    val ref = (property as KProperty1<Any, Any>).getDelegate(obj) as CommandLineValue<Any>
                    if (services.containsKey(ref.handlerServiceName) && services[ref.handlerServiceName] is CommandLineHandler) {
                        ref.setHandlerAndRegister( services[ref.handlerServiceName] as CommandLineHandler)
                        logger().log(LogLevel.TRACE, { "${this.identity}.${property.name} = ${ref.handlerServiceName}[${ref.path}]" })
                    } else {
                        throw ApplicationInstantiationException("Cannot find a CommandLineHandler service named ${ref.handlerServiceName}, at ${this.identity}.${property.name}")
                    }
                }
            }
        }
    }

    override fun start(commandLineArgs: List<String>) {
        this._services = this.defineServices(commandLineArgs)
        logger().log(LogLevel.DEBUG, { "[${this.identity}] Application.start" })
        this.doInjections(commandLineArgs)
        super.start()
    }
}