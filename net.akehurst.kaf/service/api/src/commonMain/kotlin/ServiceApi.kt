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

package net.akehurst.kaf.service.api

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

interface Service {

}

open class Reference<T : Any>(val reference: String) : ReadOnlyProperty<Any, T> {

    private lateinit var _value: T

    override operator fun getValue(thisRef: Any, property: KProperty<*>): T {
        return this._value
    }

    open fun setValue(owner: Any, ownerIdentity:String, property: KProperty<*>, value: Any) {
        this._value = value as T
    }
}

open class ServiceReference<T : Any>(val serviceClass: KClass<*>) : Reference<T>(serviceClass.simpleName!!) {
}

inline fun <reified T : Any> serviceReference(): ServiceReference<T> {
    return ServiceReference<T>(T::class)
}