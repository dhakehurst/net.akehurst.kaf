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

import net.akehurst.kaf.common.api.Component
import net.akehurst.kaf.common.api.Identifiable
import net.akehurst.kaf.common.api.Owner
import net.akehurst.kaf.common.realisation.afComponent
import net.akehurst.kaf.technology.persistence.api.PersistentStore
import net.akehurst.kotlin.komposite.common.DatatypeRegistry
import kotlin.reflect.KClass

// TODO: improve performance
// [https://medium.com/neo4j/5-tips-tricks-for-fast-batched-updates-of-graph-structures-with-neo4j-and-cypher-73c7f693c8cc]
class PersistentStoreLocalFS(

) : PersistentStore, Component {

    private val _registry = DatatypeRegistry()

    // --- injected ---


    // --- set when config is called ---


    // --- KAF ---
    override val af = afComponent {
        port("persist") {
            provides(PersistentStore::class)
        }
        initialise = { self ->
            self.af.port["persist"].connectInternal(self)
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

    override fun <T : Identifiable> readAllIdentity(type: KClass<T>): Set<String> {
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