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

import kotlin.reflect.KClass

open class PersistenceException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}

interface PersistentStore {

    fun configure(settings: Map<String, Any>)

    fun <T : Any> create(kclass: KClass<T>, item: T, identity: T.() -> String)
    fun <T : Any> createAll(kclass: KClass<T>, itemSet: Set<T>, identity: T.() -> String)
    fun <T : Any> read(kclass: KClass<T>, identity: String): T
    fun <T : Any> readAllIdentity(kclass: KClass<T>): Set<String>
    fun <T : Any> readAll(kclass: KClass<T>, identities: Set<String>): Set<T>
    fun <T : Any> update(kclass: KClass<T>, item: T, oldIdentity: String, newIdentity: T.() -> String)
    fun <T : Any> updateAll(kclass: KClass<T>, itemSet: Set<T>)
    fun <T : Any> delete(kclass: KClass<T>, identity: String)
    fun <T : Any> deleteAll(kclass: KClass<T>, identitySet: Set<String>)

}

interface Filter {
}

data class FilterProperty(
    val propertyName: String,
    val value: Any
) : Filter
