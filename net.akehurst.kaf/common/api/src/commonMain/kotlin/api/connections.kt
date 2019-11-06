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

import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

inline fun <reified T : Any> externalConnection(): ExternalConnection<T> {
    return ExternalConnection(T::class)
}

open class ExternalConnection<T : Any>(val class_:KClass<T>) : ReadWriteProperty<Any, T> {

    private lateinit var _value: T

    override operator fun getValue(thisRef: Any, property: KProperty<*>): T {
        return this._value
    }
    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        this._value = value
    }
    fun setValue( value: T) {
        this._value = value
    }
}