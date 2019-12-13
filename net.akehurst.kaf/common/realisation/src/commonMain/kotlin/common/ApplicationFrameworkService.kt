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
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

expect fun <T> runBlocking(context: CoroutineContext = EmptyCoroutineContext, block:suspend () -> T) : T

expect class ApplicationFrameworkServiceDefault : ApplicationFrameworkService {
    override fun partsOf(composite: Owner) : List<Passive>
    override fun <T : Any> proxy(forInterface: KClass<*>, invokeMethod: (handler:Any, proxy: Any?, callable: KCallable<*>, methodName:String, args: Array<out Any>) -> Any?): T
    override fun makeAccessible(callable: KCallable<*>)
    override fun callOn(obj:Any, callableName:String) : Any
    override fun doInjections(commandLineArgs: List<String>, root: AFHolder)
    override fun externalConnections(self: Passive, kclass: KClass<*>): Map<KProperty<*>, ExternalConnection<*>>
    override fun shutdown()
    override fun terminate()
}

