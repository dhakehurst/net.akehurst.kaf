package net.akehurst.kaf.common

import net.akehurst.kaf.api.Application
import net.akehurst.kaf.api.AFApplication
import net.akehurst.kaf.api.Identifiable
import net.akehurst.kaf.api.Port
import net.akehurst.kaf.service.api.Service
import net.akehurst.kaf.service.api.ServiceReference
import net.akehurst.kaf.service.commandLineHandler.api.CommandLineHandler
import net.akehurst.kaf.service.commandLineHandler.api.CommandLineValue
import net.akehurst.kaf.service.configuration.api.Configuration
import net.akehurst.kaf.service.configuration.api.ConfiguredValue
import net.akehurst.kaf.service.logging.api.LogLevel
import net.akehurst.kaf.service.logging.api.Logger
import net.akehurst.kaf.service.logging.api.LoggingService
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties
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

    override val ports: Set<Port>
        get() = TODO("not implemented")

    private fun log(): Logger {
        try {
            return this.log
        } catch (e:UninitializedPropertyAccessException) {
            // logger service may not be initialised yet
            val lgs = this._services["logging"] as LoggingService
            return lgs.create(this.identity)
        }
    }

    private fun doInjections(commandLineArgs: List<String>) {
        injectAfLoggers()
        this.injectServiceReferences()

        this.defineCommandLine()//.process(commandLineArgs);

        this.injectConfigurationValues()
        this.injectCommandLineArgs()
    }
    private fun injectAfLoggers() {
        log().trace { "injectServiceReferences" }

        val logProperty = this::class.memberProperties.find { it.name=="log" }
        logProperty!!.isAccessible = true
        val logRef = (logProperty as KProperty1<Any, Any>).getDelegate(this) as ServiceReference<Service>
        val logService = this.services["logging"] ?: throw ServiceNotFoundException("logging")
        logRef.setValue(this, this.identity, logProperty, logService)
        log().debug { "${this.identity}.log = service['logging']('${this.identity}')" }

        val walker = ApplicationCompositionWalker()
        walker.walkDepthFirst(this.self) { obj, property ->
            when {
                property.returnType.isSubtypeOf(Identifiable::class.starProjectedType) -> {
                    if (property is KProperty1<*, *>) {
                        property.isAccessible = true
                        val propValue = property.getter.call(obj) as Identifiable
                        val logProperty = propValue.af::class.memberProperties.find { it.name=="log" }
                        logProperty!!.isAccessible = true
                        val logRef = (logProperty as KProperty1<Any, Any>).getDelegate(propValue.af) as ServiceReference<Service>
                        val logService = this.services["logging"] ?: throw ServiceNotFoundException("logging")
                        logRef.setValue(propValue.af, propValue.af.identity, logProperty, logService)
                        log().debug { "${propValue.af.identity}.log = service['logging']('${propValue.af.identity}')" }
                    }
                }
            }
        }
    }

    private fun injectServiceReferences() {
        log().trace { "injectServiceReferences" }
        val walker = ApplicationCompositionWalker()
        walker.walkDepthFirst(this.self) { obj, property ->
            when {
                property.returnType.isSubtypeOf(Service::class.starProjectedType) -> {
                    if (property is KProperty1<*, *>) {
                        property.isAccessible = true
                        val ref = (property as KProperty1<Any, Any>).getDelegate(obj) as ServiceReference<Service>
                        val service = this.services[ref.reference] ?: throw ServiceNotFoundException(ref.reference)
                        ref.setValue(obj, obj.af.identity, property, service)
                        log().debug { "${obj.af.identity}.${property.name} = service['${ref.reference}']('${obj.af.identity}')" }
                    }
                }
            }
        }
    }

    private fun defineCommandLine() {

    }

    private fun injectConfigurationValues() {
        log().trace { "injectConfigurationValues" }
        val walker = ApplicationCompositionWalker()
        walker.walkDepthFirst(this.self) { obj, property ->
            if (property is KProperty1<*, *>) {
                property.isAccessible = true
                if ((property as KProperty1<Any, Any>).getDelegate(obj) is ConfiguredValue<*>) {
                    val ref = (property as KProperty1<Any, Any>).getDelegate(obj) as ConfiguredValue<Any>
                    if (services.containsKey(ref.configurationServiceName) && services[ref.configurationServiceName] is Configuration) {
                        ref.configuration = services[ref.configurationServiceName] as Configuration
                        val value: Any = ref.getValue(obj, property)//configuration.get(ref.path, ref.default)
                        log().debug { "${this.identity}.${property.name} = ${value}" }
                    } else {
                        throw ApplicationInstantiationException("Cannot find a Configuration service named ${ref.configurationServiceName}, at ${this.identity}.${property.name}")
                    }
                }
            }
        }
    }

    private fun injectCommandLineArgs() {
        log().trace { "injectCommandLineArgs" }
        val walker = ApplicationCompositionWalker()
        walker.walkDepthFirst(this.self) { obj, property ->
            if (property is KProperty1<*, *>) {
                property.isAccessible = true
                if ((property as KProperty1<Any, Any>).getDelegate(obj) is CommandLineValue<*>) {
                    val ref = (property as KProperty1<Any, Any>).getDelegate(obj) as CommandLineValue<Any>
                    if (services.containsKey(ref.handlerServiceName) && services[ref.handlerServiceName] is CommandLineHandler) {
                        ref.setHandlerAndRegister( "${this.identity}.${property.name}", services[ref.handlerServiceName] as CommandLineHandler)
                        log().debug { "${this.identity}.${property.name} = ${ref.handlerServiceName}[${ref.path}]" }
                    } else {
                        throw ApplicationInstantiationException("Cannot find a CommandLineHandler service named ${ref.handlerServiceName}, at ${this.identity}.${property.name}")
                    }
                }
            }
        }
    }

    override fun start(commandLineArgs: List<String>) {
        this._services = this.defineServices(commandLineArgs)
        log().trace { "[${this.identity}] Application.start" }
        this.doInjections(commandLineArgs)
        super.start()
    }

}