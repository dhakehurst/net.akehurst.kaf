/**
 * Copyright (C) 2019 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.akehurst.kaf.common.realisation

import net.akehurst.kaf.common.api.*
import net.akehurst.kaf.service.api.Service
import net.akehurst.kaf.service.api.ServiceReference
import net.akehurst.kaf.service.commandLineHandler.api.CommandLineHandlerService
import net.akehurst.kaf.service.commandLineHandler.api.CommandLineValue
import net.akehurst.kaf.service.configuration.api.ConfigurationService
import net.akehurst.kaf.service.configuration.api.ConfiguredValue
import net.akehurst.kaf.service.logging.api.Logger
import net.akehurst.kaf.service.logging.api.LoggingService
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.*
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.kotlinFunction

actual fun <T> runBlocking(context: CoroutineContext, block: suspend () -> T): T = kotlinx.coroutines.runBlocking(context) { block() }

actual class ApplicationFrameworkServiceDefault(
        val afId: String,
        val applicationAF: AFApplication,
        val applicationServices: Map<KClass<*>, Service>
) : ApplicationFrameworkService {

    private var logger: Logger? = null

    private fun log(): Logger {
        if (null == this.logger) {
            // logger service may not be initialised yet
            val lgs = this.applicationServices[LoggingService::class] as LoggingService? ?: throw ApplicationInstantiationException("No logging service defined")
            this.logger = lgs.create(this.afId)
        }
        return this.logger!!
    }

    actual override fun partsOf(composite: Owner): List<Passive> {
        val walker = ApplicationCompositionWalker()
        val list = mutableListOf<Passive>()
        walker.walkAfParts(composite) { part, property ->
            list.add(part)
        }
        return list
    }

    actual override fun makeAccessible(callable: KCallable<*>) {
        callable.isAccessible = true
    }

    actual override fun callOn(obj:Any, callableName:String) : Any {
        return obj::class.members.first { it.name==callableName }
    }
/*
    actual override fun <T : Any> proxy(forInterface: KClass<*>, invokeMethod: (handler:Any, proxy: Any?, callable: KCallable<*>, methodName:String, args: Array<out Any>) -> Any?): T {
        val handler = object: InvocationHandler {
            override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any? {
                val args2 = args ?: emptyArray<Any>()
                //this throws an error if one of the parameters is an inline class
                // see [https://youtrack.jetbrains.com/issue/KT-34024]
//              val callable = method?.kotlinFunction!!

                //FIXME using workaround by just checking for method name, might fail if duplicate names
                val callable = method?.declaringClass?.kotlin?.members?.firstOrNull {
                    //need to handle jvm name mangling!!

                    if (it.name.contains('-') or method.name.contains('-') ) {
                        it.name.substringBefore('-') == method.name.substringBefore('-')
                    } else {
                        it.name == method.name
                    }

                } ?: throw ApplicationFrameworkServiceException("method not found $method")
                return invokeMethod.invoke(this, proxy, callable, method.name, args2)
            }
        }
        val proxy = Proxy.newProxyInstance(forInterface.java.classLoader, arrayOf(forInterface.java), handler)
        return proxy as T
    }
 */

    actual override fun doInjections(commandLineArgs: List<String>, root: AFHolder) {
        this.setupAF(root)
        this.injectServiceReferences(root)
        injectAfLoggers(root)

        this.defineCommandLine()//.process(commandLineArgs);

        this.injectConfigurationValues(root)
        this.injectCommandLineArgs(root)
    }

    actual override fun externalConnections(self: Passive, kclass: KClass<*>): Map<KProperty<*>, ExternalConnection<*>> {
        return self::class.memberProperties.mapNotNull { property ->
            val del = (property as KProperty1<Any, Any>).getDelegate(self)
            if (del is ExternalConnection<*> && property.returnType.isSubtypeOf(kclass.starProjectedType)) {
               Pair(property, del)
            } else {
                null
            }
        }.associate { it }
    }

    actual override fun shutdown() {
        applicationAF.shutdown()
    }

    actual override fun terminate() {
        applicationAF.terminate()
    }

    private fun setupAF(root: AFHolder) {
        log().trace { "setupAF" }
        val walker = ApplicationCompositionWalker()
        walker.walkDepthFirst(root, { obj ->
            obj.af.afHolder = obj
        }) { obj, property ->
            when {
                property.returnType.isSubtypeOf(AFHolder::class.starProjectedType) -> {
                    try {
                        if (property is KProperty1<*, *> && property.visibility != KVisibility.PRIVATE) {
                            property.isAccessible = true
                            val propValue = property.getter.call(obj)
                            if (null != propValue) {
                                if (propValue is AFHolder) {
                                    if (null == propValue.af.selfIdentity) {
                                        propValue.af.selfIdentity = property.name
                                    }
                                }
                                if (propValue is Passive && obj.af is AFOwner) {
                                    propValue.af.owner = obj.af as AFOwner
                                }
                            }
                        }
                    } catch (t:Throwable) {
                        log().error(t) { "Trying to setupAF for $property" }
                    }
                }
            }
        }

    }

    private fun injectAfLoggers(root: AFHolder) {
        log().trace { "injectAfLoggers" }
        val logService = this.applicationServices[LoggingService::class] ?: throw ServiceNotFoundException(LoggingService::class)
/*
        // inject the logger into this/the framework service
        //FIXME: should not do this here! it is done multiple times
        val logProperty = this::class.memberProperties.find { it.name == "log" }
        logProperty!!.isAccessible = true
        val logRef = (logProperty as KProperty1<Any, Any>).getDelegate(this) as ServiceReference<Service>

        logRef.setValue(this, this.afId, logProperty, logService)
        log().debug { "${this.afId}.log = service['logging']('${this.afId}')" }
*/
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
                property.returnType.isSubtypeOf(AFHolder::class.starProjectedType) -> {
                    if (property is KProperty1<*, *> && property.visibility != KVisibility.PRIVATE) {
                        property.isAccessible = true
                        val propValue = property.getter.call(obj)
                        if (null != propValue && propValue is AFHolder) {
                            val logProperty = propValue.af::class.memberProperties.find { it.name == "log" }
                            logProperty!!.isAccessible = true
                            val logRef = (logProperty as KProperty1<Any, Any>).getDelegate(propValue.af) as ServiceReference<Service>
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

    private fun injectServiceReferences(root: AFHolder) {
        log().trace { "injectServiceReferences" }

        val fwService = this.applicationServices[ApplicationFrameworkService::class] ?: throw ServiceNotFoundException(ApplicationFrameworkService::class)

        //inject services into composite parts
        val walker = ApplicationCompositionWalker()
        walker.walkDepthFirst(root, { obj ->
            val fwProperty2 = obj.af::class.memberProperties.find { it.name == "framework" }
            fwProperty2!!.isAccessible = true
            val fwRef2 = (fwProperty2 as KProperty1<Any, Any>).getDelegate(obj.af) as ServiceReference<Service>
            fwRef2.setValue(this, obj.af.identity, fwProperty2, fwService)
            log().debug { "${obj.af.identity}.af.framework = service[${ApplicationFrameworkService::class.simpleName}]('${obj.af.identity}')" }
        }) { obj, property ->
            when {
                property.returnType.isSubtypeOf(Service::class.starProjectedType) -> {
                    if (property is KProperty1<*, *>) {
                        property.isAccessible = true
                        val ref = (property as KProperty1<Any, Any>).getDelegate(obj) as ServiceReference<Service>
                        val service = this.applicationServices[ref.serviceClass] ?: throw ServiceNotFoundException(ref.serviceClass)
                        ref.setValue(obj, obj.af.identity, property, service)
                        log().debug { "${obj.af.identity}.${property.name} = service['${ref.serviceClass.simpleName}']('${obj.af.identity}')" }
                    }
                }
            }
        }
    }

    private fun defineCommandLine() {

    }

    private fun injectConfigurationValues(root: AFHolder) {
        log().trace { "injectConfigurationValues" }
        val walker = ApplicationCompositionWalker()
        walker.walkDepthFirst(root, { obj ->

        }) { obj, property ->
            val objId = obj.af.identity
            if (property is KProperty1<*, *>) {
                property.isAccessible = true
                if ((property as KProperty1<Any, Any>).getDelegate(obj) is ConfiguredValue<*>) {
                    val ref = (property as KProperty1<Any, Any>).getDelegate(obj) as ConfiguredValue<Any>
                    if (applicationServices.containsKey(ConfigurationService::class) && applicationServices[ConfigurationService::class] is ConfigurationService) {
                        ref.configuration = applicationServices[ConfigurationService::class] as ConfigurationService
                        val value: Any = ref.getValue(obj, property)//configuration.get(ref.path, ref.default)
                        log().debug { "${objId}.${property.name} = ${value}" }
                    } else {
                        throw ApplicationInstantiationException("Cannot find a defined service for ${ConfigurationService::class.simpleName}, at ${objId}.${property.name}")
                    }
                }
            }
        }
    }

    private fun injectCommandLineArgs(root: AFHolder) {
        log().trace { "injectCommandLineArgs" }
        val walker = ApplicationCompositionWalker()
        walker.walkDepthFirst(root, { obj ->

        }) { obj, property ->
            val objId = obj.af.identity
            if (property is KProperty1<*, *>) {
                property.isAccessible = true
                if ((property as KProperty1<Any, Any>).getDelegate(obj) is CommandLineValue<*>) {
                    val ref = (property as KProperty1<Any, Any>).getDelegate(obj) as CommandLineValue<Any>
                    if (applicationServices.containsKey(CommandLineHandlerService::class) && applicationServices[CommandLineHandlerService::class] is CommandLineHandlerService) {
                        ref.setHandlerAndRegister("${objId}.${property.name}", applicationServices[CommandLineHandlerService::class] as CommandLineHandlerService)
                        log().debug { "${objId}.${property.name} = ${CommandLineHandlerService::class.simpleName!!}[${ref.path}]" }
                    } else {
                        throw ApplicationInstantiationException("Cannot find a defined service for ${CommandLineHandlerService::class.simpleName!!}, at ${objId}.${property.name}")
                    }
                }
            }
        }
    }


}