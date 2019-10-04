package net.akehurst.kaf.common

import net.akehurst.kaf.api.Identifiable
import net.akehurst.kaf.service.api.Service
import net.akehurst.kaf.service.api.ServiceReference
import net.akehurst.kaf.service.commandLineHandler.api.CommandLineHandler
import net.akehurst.kaf.service.commandLineHandler.api.CommandLineValue
import net.akehurst.kaf.service.configuration.api.Configuration
import net.akehurst.kaf.service.configuration.api.ConfiguredValue
import net.akehurst.kaf.service.logging.api.Logger
import net.akehurst.kaf.service.logging.api.LoggingService
import net.akehurst.kaf.service.logging.api.logger
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.isAccessible

actual class ApplicationFrameworkService(
        val afId: String,
        val applicationServices: Map<String, Service>
) : Service {

    private val log by logger("logging")

    private fun log(): Logger {
        try {
            return this.log
        } catch (e: UninitializedPropertyAccessException) {
            // logger service may not be initialised yet
            val lgs = this.applicationServices["logging"] as LoggingService
            return lgs.create(this.afId)
        }
    }

    actual fun doInjections(commandLineArgs: List<String>, root: Identifiable) {
        injectAfLoggers(root)
        this.injectServiceReferences(root)

        this.defineCommandLine()//.process(commandLineArgs);

        this.injectConfigurationValues(root)
        this.injectCommandLineArgs(root)
    }

    private fun injectAfLoggers(root: Identifiable) {
        log().trace { "injectServiceReferences" }

        // inject the logger into this/the framework service
        //FIXME: should not do this here! it is done multiple times
        val logProperty = this::class.memberProperties.find { it.name == "log" }
        logProperty!!.isAccessible = true
        val logRef = (logProperty as KProperty1<Any, Any>).getDelegate(this) as ServiceReference<Service>
        val logService = this.applicationServices["logging"] ?: throw ServiceNotFoundException("logging")
        logRef.setValue(this, this.afId, logProperty, logService)
        log().debug { "${this.afId}.log = service['logging']('${this.afId}')" }

        //inject logger into root.af
        val logProperty2 = root.af::class.memberProperties.find { it.name == "log" }
        logProperty2!!.isAccessible = true
        val logRef2 = (logProperty2 as KProperty1<Any, Any>).getDelegate(root.af) as ServiceReference<Service>
        logRef2.setValue(this, root.af.identity, logProperty2, logService)
        log().debug { "${root.af.identity}.log = service['logging']('${root.af.identity}')" }

        //inject logger into composite parts
        val walker = ApplicationCompositionWalker()
        walker.walkDepthFirst(root, { obj ->

        }) { obj, property ->
            when {
                property.returnType.isSubtypeOf(Identifiable::class.starProjectedType) -> {
                    if (property is KProperty1<*, *>) {
                        property.isAccessible = true
                        val propValue = property.getter.call(obj)
                        if (null != propValue && propValue is Identifiable) {
                            val logProperty = propValue.af::class.memberProperties.find { it.name == "log" }
                            logProperty!!.isAccessible = true
                            val logRef = (logProperty as KProperty1<Any, Any>).getDelegate(propValue.af) as ServiceReference<Service>
                            val logService = this.applicationServices["logging"] ?: throw ServiceNotFoundException("logging")
                            logRef.setValue(propValue.af, propValue.af.identity, logProperty, logService)
                            log().debug { "${propValue.af.identity}.af.log = service['logging']('${propValue.af.identity}')" }
                        } else {
                            log().debug { "${property} is null, nothing more to do here" }
                        }
                    }
                }
            }
        }
    }

    private fun injectServiceReferences(root: Identifiable) {
        log().trace { "injectServiceReferences" }

        val fwService = this.applicationServices["framework"] ?: throw ServiceNotFoundException("framework")

        //inject services into composite parts
        val walker = ApplicationCompositionWalker()
        walker.walkDepthFirst(root, { obj ->
            val fwProperty2 = obj.af::class.memberProperties.find { it.name == "framework" }
            fwProperty2!!.isAccessible = true
            val fwRef2 = (fwProperty2 as KProperty1<Any, Any>).getDelegate(obj.af) as ServiceReference<Service>
            fwRef2.setValue(this, obj.af.identity, fwProperty2, fwService)
            log().debug { "${obj.af.identity}.framework = service['framework']('${obj.af.identity}')" }
        }) { obj, property ->
            when {
                property.returnType.isSubtypeOf(Service::class.starProjectedType) -> {
                    if (property is KProperty1<*, *>) {
                        property.isAccessible = true
                        val ref = (property as KProperty1<Any, Any>).getDelegate(obj) as ServiceReference<Service>
                        val service = this.applicationServices[ref.reference] ?: throw ServiceNotFoundException(ref.reference)
                        ref.setValue(obj, obj.af.identity, property, service)
                        log().debug { "${obj.af.identity}.${property.name} = service['${ref.reference}']('${obj.af.identity}')" }
                    }
                }
            }
        }
    }

    private fun defineCommandLine() {

    }

    private fun injectConfigurationValues(root: Identifiable) {
        log().trace { "injectConfigurationValues" }
        val walker = ApplicationCompositionWalker()
        walker.walkDepthFirst(root, { obj ->

        }) { obj, property ->
            val objId = obj.af.identity
            if (property is KProperty1<*, *>) {
                property.isAccessible = true
                if ((property as KProperty1<Any, Any>).getDelegate(obj) is ConfiguredValue<*>) {
                    val ref = (property as KProperty1<Any, Any>).getDelegate(obj) as ConfiguredValue<Any>
                    if (applicationServices.containsKey(ref.configurationServiceName) && applicationServices[ref.configurationServiceName] is Configuration) {
                        ref.configuration = applicationServices[ref.configurationServiceName] as Configuration
                        val value: Any = ref.getValue(obj, property)//configuration.get(ref.path, ref.default)
                        log().debug { "${objId}.${property.name} = ${value}" }
                    } else {
                        throw ApplicationInstantiationException("Cannot find a Configuration service named ${ref.configurationServiceName}, at ${objId}.${property.name}")
                    }
                }
            }
        }
    }

    private fun injectCommandLineArgs(root: Identifiable) {
        log().trace { "injectCommandLineArgs" }
        val walker = ApplicationCompositionWalker()
        walker.walkDepthFirst(root, { obj ->

        }) { obj, property ->
            val objId = obj.af.identity
            if (property is KProperty1<*, *>) {
                property.isAccessible = true
                if ((property as KProperty1<Any, Any>).getDelegate(obj) is CommandLineValue<*>) {
                    val ref = (property as KProperty1<Any, Any>).getDelegate(obj) as CommandLineValue<Any>
                    if (applicationServices.containsKey(ref.handlerServiceName) && applicationServices[ref.handlerServiceName] is CommandLineHandler) {
                        ref.setHandlerAndRegister("${objId}.${property.name}", applicationServices[ref.handlerServiceName] as CommandLineHandler)
                        log().debug { "${objId}.${property.name} = ${ref.handlerServiceName}[${ref.path}]" }
                    } else {
                        throw ApplicationInstantiationException("Cannot find a CommandLineHandler service named ${ref.handlerServiceName}, at ${objId}.${property.name}")
                    }
                }
            }
        }
    }


}