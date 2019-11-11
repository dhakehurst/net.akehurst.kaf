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

package net.akehurst.kaf.common.api

import net.akehurst.kaf.service.api.Service
import net.akehurst.kaf.service.logging.api.Logger
import net.akehurst.kotlinx.collections.MapNonNull
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

interface ApplicationFrameworkService : Service {
    fun partsOf(composite: Owner): List<Passive>
    /**
     * return the external connections of self that have return type assignable from kclass
     */
    fun externalConnections(self: Passive, kclass: KClass<*>): Map<KProperty<*>, ExternalConnection<*>>

    fun doInjections(commandLineArgs: List<String>, root: AFHolder)

    fun <T : Any> proxy(forInterface: KClass<*>, invokeMethod: (handler:Any, proxy: Any?, callable: KCallable<*>, args: Array<out Any>) -> Any?): T
    /**
     * request application shutdown
     * all currently queued tasks should be finished
     */
    fun shutdown()

    /**
     * terminate application immediately
     * any queued tasks are lost, running tasks interrupted
     */
    fun terminate()
}

interface Identifiable {
    val identity: Any
}

interface AFHolder {
    val af: AF
}

interface Owner : AFHolder {
    override val af: AFOwner
}

interface Passive : Owner {
    val owner: Owner
    override val af: AFPassive
}

interface Active : Passive, Owner {
    override val af: AFActive
}

interface Actor : Active {
    override val af: AFActor
}

interface Component : Active {
    override val af: AFComponent
}

interface Port {

    val required: Map<KClass<*>, MutableSet<Any>>

    val provided: Map<KClass<*>, MutableSet<Any>>

    /**
     * return the object that realises the required interface
     */
    fun <T : Any> required(requiredInterface: KClass<T>): Set<T>

    /**
     * return the object that provides the given interface
     */
    fun <T : Any> provided(providedInterface: KClass<T>): Set<T>

    /**
     * connect the port to another at the same level, i.e. provides is matched to requires in each direction
     */
    fun connect(other: Port)

    /**
     * connect the port to another at the same level, i.e. provides is matched to to @ExternalConnection fields and requires matched to implemented interfaces
     */
    fun connect(other: Passive)

    /**
     * connect the port to another internal port, i.e. provides is matched to provides and requires matched to requires
     */
    fun connectInternal(internal: Port)

    /**
     * connect the port to an internal object, i.e. provides is matched to implemented interface and requires matched to @ExternalConnection fields
     */
    fun connectInternal(internal: Passive)

    fun <T : Any> provideRequired(interfaceType: KClass<out T>, provider: T)
    fun <T : Any> provideProvided(interfaceType: KClass<out T>, provider: T)
}

interface Application : Owner {
    override val af: AFApplication
}

interface AF : Identifiable {
    override val identity: String
    val framework : ApplicationFrameworkService
}

interface AFOwner : AF {
}

interface AFPassive : AFOwner {
    val self: Passive
    val owner: AFOwner?
    val log: Logger
    fun externalConnections(kClass: KClass<*>): Map<KProperty<*>, ExternalConnection<*>>
    fun doInjections(root: Passive)
}

interface AFActive : AFPassive, AFOwner {
    override val self: Active
    suspend fun initialise()
    suspend fun start()
    suspend fun join()
    suspend fun shutdown()
    suspend fun terminate()
    fun <T : Any> receiver(forInterface: KClass<T>): T
}

interface AFActor : AFActive {
    override val self: Actor
    /** for placing behavior directly on the actors processing queue */
    fun receive(callable: KCallable<*>, context: AsyncCallContext, action: suspend () -> Unit)

    suspend fun send(toSend: suspend () -> Unit): AsyncCall
}

interface AFComponent : AFActive, AFOwner {
    override val self: Component
    val port: MapNonNull<String, Port>
    fun <T : Any> portOut(requiredInterface: KClass<T>): T
    fun <T : Any> portIn(providedInterface: KClass<T>): T
}

interface AFApplication : AFOwner {
    val self: Application
    fun <T : Service> service(serviceClass: KClass<T>): T

    fun startAsync(commandLineArgs: List<String>)

    fun startBlocking(commandLineArgs: List<String>)

    /**
     * stop the application (politely) and execute the 'finalise' behaviour,
     * parts are shutdown first, with the parent 'waiting' until all parts are shutdown
     */
    fun shutdown()

    /**
     * stop the application (forcefully) and do not execute the 'finalise' behaviour,
     * do not wait for parts to shutdown
     */
    fun terminate()
}