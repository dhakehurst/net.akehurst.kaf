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
import net.akehurst.kaf.technology.persistence.api.PersistenceException
import net.akehurst.kaf.technology.persistence.api.PersistentStore
import net.akehurst.kotlin.komposite.api.PrimitiveMapper
import net.akehurst.kotlin.komposite.common.DatatypeRegistry
import net.akehurst.kotlin.komposite.common.WalkInfo
import net.akehurst.kotlin.komposite.common.kompositeWalker
import net.akehurst.kotlinx.collections.Stack
import org.neo4j.driver.v1.AuthTokens
import org.neo4j.driver.v1.Driver
import org.neo4j.driver.v1.GraphDatabase
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
class PersistentStoreNeo4j(
        afId: String
) : PersistentStore, Component {

    private val _registry = DatatypeRegistry()

    // --- injected ---
    val embeddedNeo4jDirectory: String by configuredValue("configuration") { ".neo4j" }
    val embeddedNeo4jAddress: String by configuredValue("configuration") { "localhost" }
    val embeddedNeo4jPort: Int by configuredValue("configuration") { 7777 }

    // --- set when config is called ---
    private var _neo4jService: GraphDatabaseService? = null
    private lateinit var _neo4j: Driver
    private lateinit var neo4JReader: Neo4JReader

    private fun <T : Identifiable> createCypherMergeStatements(rootItem: T): List<CypherStatement> {
        val rootIdentity = rootItem.identity
        val rootPath = listOf(rootIdentity)
        var currentObjStack = Stack<Any>()
        val walker = kompositeWalker<List<String>, List<CypherStatement>>(this._registry) {
            nullValue { path, info ->
                af.log.debug { "walk: nullValue: $path = null" }
                currentObjStack.push(CypherValue(null))
                WalkInfo(path, info.acc)
            }
            primitive { path, info, primitive, mapper ->
                af.log.debug { "walk: primitive: $path, $info" }
                val cypherValue = if (null == mapper) {
                    CypherValue(primitive)
                } else {
                    val cy = (mapper as PrimitiveMapper<Any, Any>?)?.toRaw?.invoke(primitive) ?: throw PersistenceException("Do not know how to convert ${primitive::class} to json, did you register its converter")
                    val raw = mapper.toRaw(primitive)
                    CypherValue(raw)
                }
                currentObjStack.push(cypherValue)
                WalkInfo(info.up, info.acc)
            }
            reference { path, info, value, property ->
                af.log.debug { "walk: reference: $path, $info" }
                val fromLabel = ""
                val fromId = ""
                val relLabel = ""
                val toLabel = ""
                val toId = ""
                val stm = CypherReference(fromLabel, fromId, relLabel, toLabel, toId)
                currentObjStack.push(stm)
                WalkInfo(info.up, info.acc + stm)
            }
            collBegin { path, info, type, coll ->
                af.log.debug { "walk: collBegin: $path, $info" }
                val objId = (rootPath + path).joinToString("/", "/")
                val stm = when {
                    type.isList -> CypherList(objId, coll.size)
                    type.isSet -> CypherSet(objId, coll.size)
                    else -> throw PersistenceException("Collection type ${type.name} is not supported")
                }
                currentObjStack.push(stm)
                WalkInfo(info.up, info.acc + stm)
            }
            collElementEnd { path, info, element ->
                val element = currentObjStack.pop()
                val cyColl = currentObjStack.peek()
                when (element) {
                    is CypherValue -> {
                        when (cyColl) {
                            is CypherList -> {
                                cyColl.addPrimitiveElement(element)
                            }
                            is CypherSet -> {
                                cyColl.addPrimitiveElement(element)
                            }
                            else -> throw PersistenceException("CypherCollection type ${cyColl::class.simpleName} is not supported")
                        }
                        WalkInfo(info.up, info.acc)
                    }
                    is CypherElement -> {
                        var acc = info.acc
                        when (cyColl) {
                            is CypherList -> {
                                //TODO 'index'
                                var el = CypherReference(cyColl.label, cyColl.path, CypherStatement.ELEMENT_RELATION, element.label, element.path)
                                acc += el
                            }
                            is CypherSet -> {
                                var el = CypherReference(cyColl.label, cyColl.path, CypherStatement.ELEMENT_RELATION, element.label, element.path)
                                acc += el
                            }
                            else -> throw PersistenceException("CypherCollection type ${cyColl::class.simpleName} is not supported")
                        }
                        WalkInfo(info.up, acc)
                    }
                    else -> throw PersistenceException("Collection element type ${element::class.simpleName} is not supported")
                }
            }
            mapBegin { path, info, map ->
                af.log.debug { "walk: mapBegin: $path, $info" }
                val objId = (rootPath + path).joinToString("/", "/")
                val stm = CypherMap(objId, map.size)
                currentObjStack.push(stm)
                WalkInfo(info.up, info.acc + stm)
            }
            mapEntryKeyEnd { path, info, entry ->
                //val cyKey = currentObjStack.pop()
                WalkInfo(info.up, info.acc)
            }
            mapEntryValueEnd { path, info, entry ->
                var acc = info.acc
                val cyValue = currentObjStack.pop()
                val cyKey = currentObjStack.pop()
                val cyMap = currentObjStack.peek() as CypherMap
                val entryPath = (rootPath + path).dropLast(1).joinToString("/", "/")
                val valuePath = (rootPath + path).joinToString("/", "/")
                val cyEntry = CypherMapEntry(entryPath)
                acc += cyEntry
                val cyEntryRel = CypherReference(cyMap.label, cyMap.path, CypherStatement.ENTRY_RELATION, cyEntry.label, cyEntry.path)
                acc += cyEntryRel
                when (cyKey) {
                    is CypherValue -> {
                        cyEntry.primitiveKey = cyKey
                    }
                    is CypherElement -> {
                        var el = CypherReference(cyEntry.label, cyEntry.path, CypherStatement.KEY_RELATION, cyKey.label, cyKey.path)
                        acc += el
                    }
                    else -> throw PersistenceException("Collection element type ${cyKey::class.simpleName} is not supported")
                }
                when (cyValue) {
                    is CypherValue -> {
                        TODO()
                        cyEntry.primitiveValue = cyValue
                    }
                    is CypherElement -> {
                        var el = CypherReference(cyEntry.label, cyEntry.path, CypherStatement.VALUE_RELATION, cyValue.label, cyValue.path)
                        acc += el
                    }
                    else -> throw PersistenceException("Collection element type ${cyValue::class.simpleName} is not supported")
                }
                WalkInfo(info.up, acc)
            }
            objectBegin { path, info, obj, datatype ->
                af.log.debug { "walk: objectBegin: $path, $info" }
                val objId = (rootPath + path).joinToString("/", "/")
                val objLabel = datatype.qualifiedName(".")
                val obj = CypherObject(objLabel, objId)
                currentObjStack.push(obj)
                WalkInfo(info.up, info.acc + obj)
            }
            objectEnd { path, info, obj, datatype ->
                af.log.debug { "walk: objectEnd: $path, $info" }

                WalkInfo(info.up, info.acc)
            }
            propertyBegin { path, info, property ->
                af.log.debug { "walk: propertyBegin: $path, $info" }
                info
            }
            propertyEnd { path, info, property ->
                val key = path.last()
                af.log.debug { "walk: propertyEnd: $path, $info" }
                val value = currentObjStack.pop()
                val cuObj = currentObjStack.peek() as CypherObject
                var acc = info.acc
                when (value) {
                    is CypherValue -> cuObj.properties.add(CypherProperty(key, value))
                    is CypherList -> {
                        val parent = currentObjStack.peek() as CypherObject
                        val comp = if (property.isComposite) {
                            CypherComposite(parent.label, parent.path, property.name, value.label, value.path)
                        } else {
                            CypherReference(parent.label, parent.path, property.name, value.label, value.path)
                        }
                        acc += comp
                    }
                    is CypherSet -> {
                        val parent = currentObjStack.peek() as CypherObject
                        val comp = if (property.isComposite) {
                            CypherComposite(parent.label, parent.path, property.name, value.label, value.path)
                        } else {
                            CypherReference(parent.label, parent.path, property.name, value.label, value.path)
                        }
                        acc += comp
                    }
                    is CypherMap -> {
                        val parent = currentObjStack.peek() as CypherObject
                        val comp = if (property.isComposite) {
                            CypherComposite(parent.label, parent.path, property.name, value.label, value.path)
                        } else {
                            CypherReference(parent.label, parent.path, property.name, value.label, value.path)
                        }
                        acc += comp
                    }
                    is CypherReference -> {
                    }
                    is CypherObject -> {
                        val parent = currentObjStack.peek() as CypherObject
                        val comp = CypherComposite(parent.label, parent.path, property.name, value.label, value.path)
                        acc += comp
                    }
                    else -> throw PersistenceException("Internal Error, ${value::class} not supported")
                }
                //currentObjStack.push(nObj)
                WalkInfo(info.up, acc)
            }
        }
        val result = walker.walk(WalkInfo(emptyList(), emptyList()), rootItem)
        val cypher = if (result.acc is List<*> && (result.acc as List<*>).all { it is CypherStatement }) {
            result.acc as List<CypherStatement>
        } else {
            throw PersistenceException("Internal error, List<CypherStatement> not created")
        }
        return cypher
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
        this.neo4JReader = Neo4JReader(this.af.identity + ".neo4JReader", this._neo4j)
        af.doInjections(this.neo4JReader)

        //default DateTime mapping
        val defaultPrimitiveMappers = mutableMapOf<KClass<*>, PrimitiveMapper<*,*>>()
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

        val komposite = settings["komposite"] as String
        if (settings.containsKey("primitiveMappers")) {
            defaultPrimitiveMappers.putAll( settings["primitiveMappers"] as Map<KClass<*>, PrimitiveMapper<*,*>> )
        }
        af.log.debug { "trying: to register komposite information: $komposite" }
        this._registry.registerFromConfigString(DatatypeRegistry.KOTLIN_STD, emptyMap())
        this._registry.registerFromConfigString(komposite, defaultPrimitiveMappers)
    }

    override fun <T : Identifiable> create(type: KClass<T>, item: T) {
        af.log.trace { "create(${type.simpleName}, ...)" }
        val cypherStatements = this.createCypherMergeStatements(item)
        this.executeWriteCypher(cypherStatements)
    }

    override fun <T : Identifiable> createAll(type: KClass<T>, itemSet: Set<T>) {
        af.log.trace { "createAll(${type.simpleName}, ...)" }
        val cypherStatements = itemSet.flatMap { item ->
            this.createCypherMergeStatements(item)
        }
        this.executeWriteCypher(cypherStatements)
    }

    override fun <T : Identifiable> read(type: KClass<T>, identity: Any): T {
        af.log.trace { "read(${type.simpleName}, $identity)" }
        val fromNeo4JConverter = FromNeo4JConverter(this.neo4JReader, this._neo4j.session().typeSystem())
        val dt = this._registry.findDatatypeByClass(type) ?: throw PersistenceException("type ${type.simpleName} is not registered, is the komposite configuration correct")
        val item = fromNeo4JConverter.convertRootObject(dt, identity)
        return item as T
    }

    override fun <T : Identifiable> readAll(type: KClass<T>, identities: Set<Any>): Set<T> {
        af.log.trace { "readAll(${type.simpleName}, $identities)" }
        val fromNeo4JConverter = FromNeo4JConverter(this.neo4JReader, this._neo4j.session().typeSystem())
        val dt = this._registry.findDatatypeByClass(type) ?: throw PersistenceException("type ${type.simpleName} is not registered, is the komposite configuration correct")
        val itemSet = emptySet<T>() //this.convertObjects(dt, records, rootNodeName)
        return itemSet as Set<T>
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