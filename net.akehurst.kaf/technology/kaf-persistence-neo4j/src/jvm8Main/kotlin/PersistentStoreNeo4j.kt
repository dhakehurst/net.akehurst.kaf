package net.akehurst.kaf.technology.persistence.neo4j

import com.soywiz.klock.DateTime
import net.akehurst.kaf.technology.persistence.api.PersistentStore
import kotlin.reflect.KClass
import net.akehurst.kaf.api.Component
import net.akehurst.kaf.common.afComponent
import net.akehurst.kaf.service.configuration.api.configuredValue
import net.akehurst.kaf.technology.persistence.api.Filter
import net.akehurst.kaf.technology.persistence.api.FilterProperty
import net.akehurst.kaf.technology.persistence.api.PersistenceException
import net.akehurst.kotlin.komposite.api.Datatype
import net.akehurst.kotlin.komposite.common.*
import net.akehurst.kotlinx.collections.Stack
import org.neo4j.driver.v1.*
import org.neo4j.driver.v1.types.TypeSystem
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.kernel.configuration.BoltConnector
import java.io.File

interface CypherStatement {
    companion object {
        val PATH_PROPERTY = "`#path`"
        val COMPOSITE_PROPERTY = "`#isComposite`"
    }

    fun toCypherString(): String
}

data class CypherValue(
        val value: Any?
) {
    fun toCypherString(): String {
        return when {
            null == value -> "NULL"
            value is String -> "'$value'"
            value is Boolean -> "$value"
            value is Int -> "$value"
            value is Long -> "$value"
            value is Float -> "$value"
            value is Double -> "$value"
            value is List<*> -> {
                val elements = value.map {
                    if (it is CypherValue) {
                        it.toCypherString()
                    } else {
                        CypherValue(it).toCypherString()
                    }
                }.joinToString(",")
                "[$elements]"
            }
            // TODO: support user defined primitive type mappers
            value is DateTime -> "datetime('${value.toString("yyyy-MM-dd'T'HH:mm:ss")}')" //TODO: timezone and more resolution on seconds
            else -> throw PersistenceException("CypherValue of type ${value::class.simpleName} is not yet supported")
        }
    }
}

class CypherList(
        val objectIdentity: String
) : CypherStatement {

    val label = "#LIST"

    override fun toCypherString(): String {
        return "MERGE (:`$label`{${CypherStatement.PATH_PROPERTY}:'$objectIdentity'})"
    }
}

class CypherSet(
        val objectIdentity: String
) : CypherStatement {

    val label = "#SET"

    override fun toCypherString(): String {
        return "MERGE (:`$label`{${CypherStatement.PATH_PROPERTY}:'$objectIdentity'})"
    }
}

class CypherMap(
        val objectIdentity: String
) : CypherStatement {

    val label = "#MAP"

    override fun toCypherString(): String {
        return "MERGE (:`$label`{${CypherStatement.PATH_PROPERTY}:'$objectIdentity'})"
    }
}
//TODO: needs id props to identify from and to or match path from root and root has id prop!
class CypherReference(
        val fromLabel: String,
        val fromId: String,
        val relLabel: String,
        val toLabel: String,
        val toId: String
) : CypherStatement {

    override fun toCypherString(): String {
        return "MERGE (:`$fromLabel`{${CypherStatement.PATH_PROPERTY}:'$fromId'})-[r:`$relLabel`]->(:`$toLabel`{${CypherStatement.PATH_PROPERTY}:'$toId'})"
    }
}

//TODO: needs id props to identify from and to or match path from root and root has id prop!
class CypherComposite(
        val fromLabel: String,
        val fromId: String,
        val relLabel: String,
        val toLabel: String,
        val toId: String
) : CypherStatement {

    override fun toCypherString(): String {
        val idProps = ""
        return "MERGE (:`$fromLabel`{${CypherStatement.PATH_PROPERTY}:'$fromId'})-[r:`$relLabel`{${CypherStatement.COMPOSITE_PROPERTY}=true}]->(:`$toLabel`{${CypherStatement.PATH_PROPERTY}:'$toId'})"
    }
}

data class CypherProperty(val name: String, val value: CypherValue) {
    fun toCypherString(): String = "${name}: ${value.toCypherString()}"
}

data class CypherObject(
        val label: String,
        val objectPath: String
) : CypherStatement {

    val properties = mutableListOf<CypherProperty>()

    override fun toCypherString(): String {
        val propertyStr = this.properties.filter { null!=it.value.value }.map { it.toCypherString() }.joinToString(", ")
        return if (propertyStr.isEmpty()) {
            "MERGE (:`$label`{${CypherStatement.PATH_PROPERTY}:'$objectPath'})"
        } else {
            "MERGE (:`$label`{${CypherStatement.PATH_PROPERTY}:'$objectPath', $propertyStr})"
        }
    }
}

data class CypherMatchNode(
        val label: String,
        val nodeName: String
) : CypherStatement {
    val properties = mutableListOf<CypherProperty>()
    override fun toCypherString(): String {
        val propertyStr = this.properties.map { it.toCypherString() }.joinToString(", ")
        return if (propertyStr.isEmpty()) {
            "MATCH ($nodeName:`$label`) RETURN $nodeName"
        } else {
            "MATCH ($nodeName:`$label`{$propertyStr}) RETURN $nodeName"
        }
    }
}

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

    private fun <T : Any> createCypherMergeStatements(item: T): List<CypherStatement> {
        var currentObjStack = Stack<Any>()
        val walker = kompositeWalker<List<String>, List<CypherStatement>>(this._registry) {
            nullValue { path, info ->
                af.log.debug { "walk: nullValue: $path = null" }
                currentObjStack.push(CypherValue(null))
                WalkInfo(path, info.acc)
            }
            primitive { path, info, value ->
                af.log.debug { "walk: primitive: $path, $info" }
                val cypherValue = CypherValue(value)
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
                val objId = path.joinToString("/", "/")
                val stm = when {
                    type.isList -> CypherList(objId)
                    type.isSet -> CypherSet(objId)
                    else -> throw PersistenceException("Collection type ${type.name} is not supported")
                }
                currentObjStack.push(stm)
                WalkInfo(info.up, info.acc + stm)
            }
            mapBegin { path, info, map ->
                af.log.debug { "walk: mapBegin: $path, $info" }
                val objId = path.joinToString("/", "/")
                val stm = CypherMap(objId)
                currentObjStack.push(stm)
                WalkInfo(info.up, info.acc + stm)
            }
            mapEntryKeyEnd { path, info, entry ->
                val cyKey = currentObjStack.pop()
                WalkInfo(info.up, info.acc)
            }
            mapEntryValueEnd { path, info, entry ->
                val cyValue = currentObjStack.pop()
                WalkInfo(info.up, info.acc)
            }
            objectBegin { path, info, obj, datatype ->
                af.log.debug { "walk: objectBegin: $path, $info" }
                val objId = path.joinToString("/", "/")
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
                        val fromLabel = parent.label
                        val fromId = parent.objectPath
                        val relLabel = property.name
                        val toLabel = value.label
                        val toId = value.objectIdentity
                        val comp = if (property.isComposite) {
                            CypherComposite(fromLabel, fromId, relLabel, toLabel, toId)
                        } else {
                            CypherReference(fromLabel, fromId, relLabel, toLabel, toId)
                        }
                        acc += comp
                    }
                    is CypherSet -> {
                        val set = value
                    }
                    is CypherMap -> {
                        val map = value
                    }
                    is CypherReference -> {
                    }
                    is CypherObject -> {
                        val parent = currentObjStack.peek() as CypherObject
                        val fromLabel = parent.label
                        val fromId = parent.objectPath
                        val relLabel = ""
                        val toLabel = value.label
                        val toId = value.objectPath
                        val comp = CypherComposite(fromLabel, fromId, relLabel, toLabel, toId)
                        acc += comp
                    }
                    else -> throw PersistenceException("Internal Error, ${value::class} not supported")
                }
                //currentObjStack.push(nObj)
                WalkInfo(info.up, acc)
            }
        }
        val result = walker.walk(WalkInfo(emptyList(), emptyList()), item)
        val cypher = if (result.acc is List<*> && (result.acc as List<*>).all { it is CypherStatement }) {
            result.acc as List<CypherStatement>
        } else {
            throw PersistenceException("Internal error, List<CypherStatement> not created")
        }
        return cypher
    }

    private fun createCypherMatchStatements(datatype: Datatype, filterSet: Set<Filter>, rootNodeName: String): List<CypherStatement> {
        val label = datatype.qualifiedName(".")
        //TODO: handle composition and reference!
        val cypherStatement = CypherMatchNode(label, rootNodeName)
        filterSet.forEach { filter ->
            when (filter) {
                is FilterProperty -> {
                    cypherStatement.properties.add(CypherProperty(filter.propertyName, CypherValue(filter.value)))
                }
                else -> throw PersistenceException("Filter type ${filter::class.simpleName} is not yet supported")
            }
        }
        return listOf(cypherStatement)
    }

    private fun executeWriteCypher(cypherStatements: List<CypherStatement>) {
        //TODO: use 'USING PERIODIC COMMIT' to improve performance
        this._neo4j.session().use { session ->
            session.writeTransaction { tx ->
                cypherStatements.forEach { stm ->
                    val cypherStr = stm.toCypherString()
                    af.log.trace { "executeWriteCypher($cypherStr)" }
                    val result = tx.run(cypherStr)
                }
            }
        }
    }

    private fun executeReadCypher(cypherStatements: List<CypherStatement>): List<Record> {
        //TODO: use 'USING PERIODIC COMMIT' to improve performance
        val records = mutableListOf<Record>()
        this._neo4j.session().use { session ->
            session.readTransaction { tx ->
                cypherStatements.forEach { stm ->
                    val cypherStr = stm.toCypherString()
                    af.log.trace { "executeReadCypher($cypherStr)" }
                    val result = tx.run(cypherStr)
                    records.addAll(result.list())
                }
            }
        }
        return records
    }

    fun convertValue(ts: TypeSystem, neo4jValue: Value): Any? {
        return when (neo4jValue.type()) {
            ts.NULL() -> null
            ts.STRING() -> neo4jValue.asString()
            ts.INTEGER() -> neo4jValue.asInt()
            ts.BOOLEAN() -> neo4jValue.asBoolean()
            ts.LIST() -> neo4jValue.asList()
            //ts.SET() -> neo4jValue.asSet()
            ts.MAP() -> neo4jValue.asMap()
            ts.DATE_TIME() -> {
                val dateTime = neo4jValue.asZonedDateTime()
                val unixMillis = dateTime.toInstant().toEpochMilli()
                DateTime.fromUnix(unixMillis)
            }
            else -> throw PersistenceException("Neo4j value type ${neo4jValue.type()} is not yet supported")
        }
    }

    private fun convertObjects(datatype: Datatype, records: List<Record>, nodeName: String): Set<Any> {
        return records.map {
            this.convertObject(datatype, it, nodeName)
        }.toSet()
    }

    private fun convertObject(datatype: Datatype, record: Record, nodeName: String): Any {
        val ts = this._neo4j.session().typeSystem()
        val node = record[nodeName].asNode()
        val idProps = datatype.identityProperties.map {
            val neo4jValue = node[it.name]
            //TODO: handel non primitive properties
            if (null == neo4jValue) {
                null
            } else {
                val v = this.convertValue(ts, neo4jValue)
                v
            }
        }
        val obj = datatype.construct(*idProps.toTypedArray()) //TODO: need better error when this fails
        //resolvedReference[path] = obj

        // TODO: change this to enable nonExplicit properties, once JS reflection works
        datatype.explicitNonIdentityProperties.forEach {
            val neo4jValue = record[it.name]
            if (null != neo4jValue) {
                val value = this.convertValue(ts, neo4jValue)
                it.set(obj, value)
            }
        }
        return obj
    }

    // --- KAF ---
    override val af = afComponent(this, afId) {
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

        val komposite = settings["komposite"] as String
        af.log.debug { "trying: to register komposite information: $komposite" }
        this._registry.registerFromConfigString(DatatypeRegistry.KOTLIN_STD)
        this._registry.registerFromConfigString(komposite)
    }

    override fun <T : Any> create(type: KClass<T>, item: T) {
        af.log.trace { "create(${type.simpleName}, ...)" }
        val cypherStatements = this.createCypherMergeStatements(item)
        this.executeWriteCypher(cypherStatements)
    }

    override fun <T : Any> createAll(type: KClass<T>, itemSet: Set<T>) {
        af.log.trace { "createAll(${type.simpleName}, ...)" }
        val cypherStatements = this.createCypherMergeStatements(itemSet)
        this.executeWriteCypher(cypherStatements)
    }

    override fun <T : Any> read(type: KClass<T>, filterSet: Set<Filter>): T {
        af.log.trace { "read(${type.simpleName}, $filterSet)" }
        val dt = this._registry.findDatatypeByClass(type) ?: throw PersistenceException("type ${type.simpleName} is not registered, is the komposite configuration correct")
        val cypherStatements = this.createCypherMatchStatements(dt, filterSet, "item")
        val records = this.executeReadCypher(cypherStatements)
        val item = this.convertObject(dt, records[0], "item")
        return item as T
    }

    override fun <T : Any> readAll(type: KClass<T>, filterSet: Set<Filter>): Set<T> {
        af.log.trace { "readAll(${type.simpleName}, $filterSet)" }
        val dt = this._registry.findDatatypeByClass(type) ?: throw PersistenceException("type ${type.simpleName} is not registered, is the komposite configuration correct")
        val cypherStatements = this.createCypherMatchStatements(dt, filterSet, "item")
        val records = this.executeReadCypher(cypherStatements)
        val itemSet = this.convertObjects(dt, records, "item")
        return itemSet as Set<T>
    }

    override fun <T : Any> update(type: KClass<T>, item: T, filterSet: Set<Filter>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T : Any> updateAll(type: KClass<T>, itemSet: Set<T>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T : Any> delete(identity: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T : Any> deleteAll(identitySet: Set<String>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}