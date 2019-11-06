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

package net.akehurst.kaf.service.configuration.api

import net.akehurst.kaf.common.api.Passive
import net.akehurst.kaf.service.api.Service
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

interface ConfigurationService : Service {
    fun <T> get(path: String, default: () -> T): T
}

fun <T : Any> configuredValue(default: () -> T): ConfiguredValue<T> {
    return ConfiguredValue<T>(null, default)
}

fun <T : Any> configuredValue(name: String, default: () -> T): ConfiguredValue<T> {
    return ConfiguredValue<T>(name, default)
}


class ConfiguredValue<T : Any>(
        val overridePropertyName: String?,
        val default: () -> T
) : ReadOnlyProperty<Passive, T> {

    lateinit var configuration: ConfigurationService

    override fun getValue(thisRef: Passive, property: KProperty<*>): T {
        val name = overridePropertyName ?: property.name
        val path = "${thisRef.af.identity}.${name}"
        return this.configuration.get(path, this.default)
    }
}