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

package net.akehurst.kaf.service.commandLineHandler.api

import net.akehurst.kaf.common.api.Passive
import net.akehurst.kaf.service.api.Service
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

interface CommandLineHandlerService : Service {
    fun <T : Any?> get(path: String, default: () -> T?): T?
    fun <T : Any> registerOption(path: String, type: KClass<T>, default: T?, description: String, hidden: Boolean)
}

inline fun <reified T : Any> commandLineValue(noinline default: () -> T?): CommandLineValue<T> {
    return CommandLineValue<T>(T::class, default)
}


class CommandLineValue<T : Any>(
        val type: KClass<T>,
        val default: () -> T?,
        val description: String = ""
) : ReadOnlyProperty<Passive, T?> {

    lateinit var path: String
    lateinit var handler: CommandLineHandlerService

    override fun getValue(thisRef: Passive, property: KProperty<*>): T? {
        return this.handler.get(this.path, this.default)
    }

    fun setHandlerAndRegister(path:String, handler: CommandLineHandlerService) {
        this.path = path
        this.handler = handler
        handler.registerOption(this.path, type, this.default(), this.description, false)
    }
}