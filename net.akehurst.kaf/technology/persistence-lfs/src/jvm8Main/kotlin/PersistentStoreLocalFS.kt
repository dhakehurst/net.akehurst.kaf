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

package net.akehurst.kaf.technology.persistence.neo4j

import com.soywiz.klock.DateTime
import net.akehurst.kaf.common.api.Component
import net.akehurst.kaf.common.api.Identifiable
import net.akehurst.kaf.common.realisation.afComponent
import net.akehurst.kaf.service.configuration.api.configuredValue
import net.akehurst.kaf.technology.persistence.api.Filter
import net.akehurst.kaf.technology.persistence.api.PersistenceException
import net.akehurst.kaf.technology.persistence.api.PersistentStore
import net.akehurst.kotlin.komposite.api.Datatype
import net.akehurst.kotlin.komposite.api.PrimitiveMapper
import net.akehurst.kotlin.komposite.common.DatatypeRegistry
import net.akehurst.kotlin.komposite.common.WalkInfo
import net.akehurst.kotlin.komposite.common.get
import net.akehurst.kotlin.komposite.common.kompositeWalker
import net.akehurst.kotlinx.collections.Stack
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.Set
import kotlin.collections.all
import kotlin.collections.dropLast
import kotlin.collections.emptyList
import kotlin.collections.emptyMap
import kotlin.collections.emptySet
import kotlin.collections.flatMap
import kotlin.collections.forEach
import kotlin.collections.joinToString
import kotlin.collections.last
import kotlin.collections.listOf
import kotlin.collections.mutableMapOf
import kotlin.collections.plus
import kotlin.collections.set
import kotlin.reflect.KClass

// TODO: improve performance
// [https://medium.com/neo4j/5-tips-tricks-for-fast-batched-updates-of-graph-structures-with-neo4j-and-cypher-73c7f693c8cc]
class PersistentStoreLocalFS(
        afId: String
) : PersistentStore, Component {

    private val _registry = DatatypeRegistry()

    // --- injected ---


    // --- set when config is called ---


    // --- KAF ---
    override val af = afComponent(this, afId) {
        port("persist") {
            provides(PersistentStore::class)
        }
        initialise = {
            self.af.port["persist"].connectInternal(self)
        }
        execute = {
        }
        terminate = {

        }
    }

    // --- PersistentStore ---
    override fun configure(settings: Map<String, Any>) {
    }

    override fun <T : Identifiable> create(type: KClass<T>, item: T) {

    }

    override fun <T : Identifiable> createAll(type: KClass<T>, itemSet: Set<T>) {

    }

    override fun <T : Identifiable> read(type: KClass<T>, identity: Any): T {
        TODO("not implemented")
    }

    override fun <T : Identifiable> readAll(type: KClass<T>, identities: Set<Any>): Set<T> {
        TODO("not implemented")
    }

    override fun <T : Identifiable> update(type: KClass<T>, item: T) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T : Identifiable> updateAll(type: KClass<T>, itemSet: Set<T>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T : Identifiable> delete(identity: Any) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T : Identifiable> deleteAll(identitySet: Set<Any>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}