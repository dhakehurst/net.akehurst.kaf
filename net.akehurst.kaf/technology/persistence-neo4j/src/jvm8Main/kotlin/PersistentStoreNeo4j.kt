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
import net.akehurst.kaf.common.api.Port
import net.akehurst.kaf.common.realisation.afComponent
import net.akehurst.kaf.service.configuration.api.configuredValue
import net.akehurst.kaf.technology.persistence.api.PersistenceException
import net.akehurst.kaf.technology.persistence.api.PersistentStore
import net.akehurst.kotlin.komposite.api.PrimitiveMapper
import net.akehurst.kotlin.komposite.common.DatatypeRegistry
import net.akehurst.kotlin.komposite.common.WalkInfo
import net.akehurst.kotlin.komposite.common.kompositeWalker
import net.akehurst.kotlinx.collections.Stack
import org.neo4j.driver.AuthTokens
import org.neo4j.driver.Driver
import org.neo4j.driver.GraphDatabase
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.kernel.configuration.BoltConnector
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
class PersistentStoreNeo4j(
) : PersistentStore, Component {

    lateinit var port_persist: Port

    private val _registry = DatatypeRegistry()

    // --- injected ---
    val embeddedNeo4jDirectory: String by configuredValue { ".neo4j" }
    val embeddedNeo4jAddress: String by configuredValue { "localhost" }
    val embeddedNeo4jPort: Int by configuredValue { 7777 }

    // --- set when config is called ---
    private var _neo4jService: GraphDatabaseService? = null
    private lateinit var _neo4j: Driver
    private lateinit var _neo4JReader: Neo4JReader

    private val neo4JReader: Neo4JReader
        get() {
            try {
                return this._neo4JReader
            } catch (t: Throwable) {
                throw PersistenceException("problem accessing neo4J database, perhaps configure has not been called with valid settings")
            }
        }

    private fun executeWriteCypher(cypherStatements: List<CypherStatement>) {
        //TODO: use 'USING PERIODIC COMMIT' to improve performance
        this._neo4j.session().use { session ->
            session.writeTransaction { tx ->
                cypherStatements.forEach { stm ->
                    val cypherStr = stm.toCypherStatement()
                    af.log.trace { "executeWriteCypher($cypherStr)" }
                    val result = tx.run(cypherStr)
                }
            }
        }
    }

    // --- KAF ---
    override val af = afComponent {
        port_persist = port("persist") {
            provides(PersistentStore::class)
        }
        initialise = { self ->
            self.af.port["persist"].connectInternal(self)
        }
        execute = {
        }
        finalise = {
            if (null != _neo4jService) {
                _neo4jService?.shutdown()
            }
        }
    }

    // --- PersistentStore ---
    override fun configure(settings: Map<String, Any>) {
        val embedded = settings["embedded"] as Boolean? ?: false
        if (embedded) {
            af.log.info { "starting embedded neo4j database in directory $embeddedNeo4jDirectory at $embeddedNeo4jAddress:$embeddedNeo4jPort" }
            val embeddedDirectory = File("${this.embeddedNeo4jDirectory}/data")
            val bolt = BoltConnector("0")
            this._neo4jService = GraphDatabaseFactory() //
                    .newEmbeddedDatabaseBuilder(embeddedDirectory)
                    .setConfig(bolt.type, "BOLT")
                    .setConfig(bolt.enabled, "true")
                    .setConfig(bolt.listen_address, "$embeddedNeo4jAddress:$embeddedNeo4jPort")
                    .newGraphDatabase()

            // Registers a shutdown hook for the Neo4j instance so that it
            // shuts down nicely when the VM exits (even if you "Ctrl-C" the
            // running application).
            Runtime.getRuntime().addShutdownHook(object : Thread() {
                override fun run() {
                    _neo4jService?.shutdown()
                }
            })
        }

        val uri = settings["uri"] as String
        val user = settings["user"] as String
        val password = settings["password"] as String
        af.log.debug { "trying: to connect to Neo4j: ${uri} as user ${user}" }
        this._neo4j = GraphDatabase.driver(uri, AuthTokens.basic(user, password))
        this._neo4j.session().use { session ->
            session.readTransaction { tx ->
                tx.run("RETURN 'Hello Neo4j'")
            }
        }
        af.log.debug { "success: connected to Neo4j: ${uri} as user ${user}" }
        this._neo4JReader = Neo4JReader(this._neo4j)
        af.doInjections(this.neo4JReader)

        //default DateTime mapping
        val defaultPrimitiveMappers = mutableMapOf<KClass<*>, PrimitiveMapper<*, *>>()
        defaultPrimitiveMappers[DateTime::class] = PrimitiveMapper.create(DateTime::class, ZonedDateTime::class,
                { primitive ->
                    //primitive.toString("yyyy-MM-dd'T'HH:mm:ss")
                    val instant = Instant.ofEpochMilli(primitive.unixMillisLong)
                    ZonedDateTime.ofInstant(instant, ZoneId.systemDefault())
                },
                { raw ->
                    val unixMillis = raw.toInstant().toEpochMilli()
                    DateTime.fromUnix(unixMillis)
                })

        val komposite = settings["komposite"] as List<String>
        if (settings.containsKey("primitiveMappers")) {
            defaultPrimitiveMappers.putAll(settings["primitiveMappers"] as Map<KClass<*>, PrimitiveMapper<*, *>>)
        }
        af.log.debug { "trying: to register komposite information: $komposite" }
        komposite.forEach {
            this._registry.registerFromConfigString(it, emptyMap())
        }
        this._registry.registerFromConfigString(DatatypeRegistry.KOTLIN_STD, defaultPrimitiveMappers)
    }

    override fun <T : Any> create(type: KClass<T>, item: T, identity: T.() -> String) {
        try {
            af.log.trace { "create(${type.simpleName}, ...)" }
            val serialiser = KSerialiserCypherStatements(this._registry)
            val cypherStatements = serialiser.createCypherMergeStatements(item, identity)
            this.executeWriteCypher(cypherStatements)
        } catch (t: Throwable) {
            throw PersistenceException("In ${this::class.simpleName}.create: ${t.message}")
        }
    }

    override fun <T : Any> createAll(type: KClass<T>, itemSet: Set<T>, identity: T.() -> String) {
        try {
            af.log.trace { "createAll(${type.simpleName}, ...)" }
            val cypherStatements = itemSet.flatMap { item ->
                val serialiser = KSerialiserCypherStatements(this._registry)
                val cypherStatements = serialiser.createCypherMergeStatements(item, identity)
                cypherStatements
            }
            this.executeWriteCypher(cypherStatements)
        } catch (t: Throwable) {
            throw PersistenceException("In ${this::class.simpleName}.createAll: ${t.message}")
        }
    }

    override fun <T : Any> read(type: KClass<T>, identity: String): T {
        try {
            af.log.trace { "read(${type.simpleName}, $identity)" }
            val fromNeo4JConverter = FromNeo4JConverter(this.neo4JReader, this._neo4j.defaultTypeSystem(), this._registry)
            val dt = this._registry.findDatatypeByClass(type) ?: throw PersistenceException("type ${type.simpleName} is not registered, is the komposite configuration correct")
            val item = fromNeo4JConverter.convertRootObject(dt, identity)
            return item as T
        } catch (t: Throwable) {
            throw PersistenceException("In ${this::class.simpleName}.read: ${t.message}")
        }
    }

    override fun <T : Any> readAllIdentity(type: KClass<T>): Set<String> {
        try {
            val fromNeo4JConverter = FromNeo4JConverter(this.neo4JReader, this._neo4j.defaultTypeSystem(), _registry)
            val dt = this._registry.findDatatypeByClass(type) ?: throw PersistenceException("type ${type.simpleName} is not registered, is the komposite configuration correct")
            val allIds = fromNeo4JConverter.fetchAllIds(dt)
            return allIds
        } catch (t: Throwable) {
            throw PersistenceException("In ${this::class.simpleName}.readAllIdentity: ${t.message}")
        }
    }

    override fun <T : Any> readAll(type: KClass<T>, identities: Set<String>): Set<T> {
        try {
            af.log.trace { "readAll(${type.simpleName}, $identities)" }
            val itemSet = identities.map {
                read(type, it)
            }.toSet()
            return itemSet
        } catch (t: Throwable) {
            throw PersistenceException("In ${this::class.simpleName}.readAll: ${t.message}")
        }
    }

    override fun <T : Any> update(type: KClass<T>, item: T, oldIdentity: String, newIdentity: T.() -> String) {
        try {
            af.log.trace { "update(${type.simpleName}, $item)" }
            this.delete<T>(type, oldIdentity)
            this.create(type, item, newIdentity)
        } catch (t: Throwable) {
            throw PersistenceException("In ${this::class.simpleName}.update: ${t.message}")
        }
    }

    override fun <T : Any> updateAll(type: KClass<T>, itemSet: Set<T>) {
        try {
            af.log.trace { "updateAll(${type.simpleName}, $itemSet)" }
            TODO()
        } catch (t: Throwable) {
            throw PersistenceException("In ${this::class.simpleName}.updateAll: ${t.message}")
        }
    }

    override fun <T : Any> delete(type: KClass<T>, identity: String) {
        try {
            af.log.trace { "delete($identity)" }
            val label = this._registry.findDatatypeByClass(type)!!.qualifiedName(".")
            val path = "/$identity"
            val cypherStatements = listOf(CypherDeleteRecursive(label, path))
            this.executeWriteCypher(cypherStatements)
        } catch (t: Throwable) {
            throw PersistenceException("In ${this::class.simpleName}.delete: ${t.message}")
        }
    }

    override fun <T : Any> deleteAll(type: KClass<T>, identitySet: Set<String>) {
        try {
            af.log.trace { "deleteAll($identitySet)" }
            TODO()
        } catch (t: Throwable) {
            throw PersistenceException("In ${this::class.simpleName}.deleteAll: ${t.message}")
        }
    }


}