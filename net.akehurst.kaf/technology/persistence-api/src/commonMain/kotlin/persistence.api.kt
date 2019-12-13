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

package net.akehurst.kaf.technology.persistence.api

import net.akehurst.kaf.common.api.Identifiable
import kotlin.reflect.KClass


open class PersistenceException : RuntimeException {
    constructor(message: String) : super(message)
}

interface PersistentStore {

    fun configure(settings: Map<String, Any>)

    fun <T : Identifiable> create(type: KClass<T>, item: T)
    fun <T : Identifiable> createAll(type: KClass<T>, itemSet: Set<T>)
    fun <T : Identifiable> read(type: KClass<T>, identity: Any): T
    fun <T : Identifiable> readAllIdentity(type: KClass<T>): Set<String>
    fun <T : Identifiable> readAll(type: KClass<T>, identities: Set<Any>): Set<T>
    fun <T : Identifiable> update(type: KClass<T>, item: T)
    fun <T : Identifiable> updateAll(type: KClass<T>, itemSet: Set<T>)
    fun <T : Identifiable> delete(identity: Any)
    fun <T : Identifiable> deleteAll(identitySet: Set<Any>)

}

interface Filter {
}

data class FilterProperty(
        val propertyName: String,
        val value: Any
) : Filter
