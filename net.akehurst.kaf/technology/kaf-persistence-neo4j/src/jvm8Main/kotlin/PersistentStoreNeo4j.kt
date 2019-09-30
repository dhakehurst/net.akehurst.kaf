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
import net.akehurst.kotlin.komposite.api.CollectionType
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
        val PATH_PROPERTY = "#path"
        val COMPOSITE_PROPERTY = "#isComposite"
        val ELEMENTS_PROPERTY = "#elements"
        val ELEMENT_RELATION = "#element"
        val SET_TYPE_LABEL = "#SET"
        val LIST_TYPE_LABEL = "#LIST"

        val MAP_TYPE_LABEL = "#MAP"
        val ENTRY_RELATION = "#entry"
        val MAPENTRY_TYPE_LABEL = "#MAPENTRY"
        val KEY_PROPERTY = "#key"
        val KEY_RELATION = "#key"
        val VALUE_RELATION = "#value"
    }

    fun toCypherStatement(): String
}

interface CypherElement : CypherStatement {
    val label: String
    val path: String
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

data class CypherList(
        override val path: String
) : CypherElement {

    private val _primitiveElements = mutableListOf<CypherValue>()

    val isPrimitiveCollection get() = this._primitiveElements.isNotEmpty()

    override val label = CypherStatement.LIST_TYPE_LABEL

    fun addPrimitiveElement(element: CypherValue) = this._primitiveElements.add(element)

    override fun toCypherStatement(): String {
        return if (this.isPrimitiveCollection) {
            val elements = this._primitiveElements.map { it.toCypherString() }.joinToString(separator = ",", prefix = "[", postfix = "]")
            "MERGE (:`$label`{`${CypherStatement.PATH_PROPERTY}`:'$path', `${CypherStatement.ELEMENTS_PROPERTY}`:$elements})"
        } else {
            "MERGE (:`$label`{`${CypherStatement.PATH_PROPERTY}`:'$path'})"
        }
    }
}

data class CypherSet(
        override val path: String
) : CypherElement {

    private val _primitiveElements = mutableListOf<CypherValue>()

    val isPrimitiveCollection get() = this._primitiveElements.isNotEmpty()

    override val label = CypherStatement.SET_TYPE_LABEL

    fun addPrimitiveElement(element: CypherValue) = this._primitiveElements.add(element)

    override fun toCypherStatement(): String {
        return if (this.isPrimitiveCollection) {
            val elements = this._primitiveElements.map { it.toCypherString() }.joinToString(separator = ",", prefix = "[", postfix = "]")
            "MERGE (:`$label`{`${CypherStatement.PATH_PROPERTY}`:'$path', `${CypherStatement.ELEMENTS_PROPERTY}`:$elements})"
        } else {
            "MERGE (:`$label`{`${CypherStatement.PATH_PROPERTY}`:'$path'})"
        }
    }
}

data class CypherMap(
        override val path: String
) : CypherElement {

    override val label = CypherStatement.MAP_TYPE_LABEL

    override fun toCypherStatement(): String {
        return "MERGE (:`$label`{`${CypherStatement.PATH_PROPERTY}`:'$path'})"
    }
}

data class CypherMapEntry(
        override val path: String
) : CypherElement {

    override val label = CypherStatement.MAPENTRY_TYPE_LABEL

    var primitiveKey: CypherValue? = null
    var primitiveValue: CypherValue? = null

    override fun toCypherStatement(): String {
        //TODO: primitive keys and values
        if (null == this.primitiveKey) {
            return "MERGE (:`$label`{`${CypherStatement.PATH_PROPERTY}`:'$path'})"
        } else {
            val kv = primitiveKey!!.toCypherString()
            return "MERGE (:`$label`{`${CypherStatement.PATH_PROPERTY}`:'$path', `${CypherStatement.KEY_PROPERTY}`:${kv} })"
        }
    }
}

//TODO: needs id props to identify from and to or match path from root and root has id prop!
class CypherReference(
        val srcLabel: String,
        val srcPath: String,
        val relLabel: String,
        val tgtLabel: String,
        val tgtPath: String
) : CypherStatement {

    override fun toCypherStatement(): String {
        return """
            MATCH (src:`$srcLabel`{`${CypherStatement.PATH_PROPERTY}`:'$srcPath'})
            MATCH (tgt:`$tgtLabel`{`${CypherStatement.PATH_PROPERTY}`:'$tgtPath'})
            MERGE (src)-[r:`$relLabel`]->(tgt)
        """
        //return "MERGE (:`$fromLabel`{`${CypherStatement.PATH_PROPERTY}`:'$fromPath'})-[r:`$relLabel`]->(:`$toLabel`{`${CypherStatement.PATH_PROPERTY}`:'$toPath'})"
    }
}

//TODO: needs id props to identify from and to or match path from root and root has id prop!
class CypherComposite(
        val srcLabel: String,
        val srcPath: String,
        val relLabel: String,
        val tgtLabel: String,
        val tgtPath: String
) : CypherStatement {

    override fun toCypherStatement(): String {
        return """
            MATCH (src:`$srcLabel`{`${CypherStatement.PATH_PROPERTY}`:'$srcPath'})
            MATCH (tgt:`$tgtLabel`{`${CypherStatement.PATH_PROPERTY}`:'$tgtPath'})
            MERGE (src)-[r:`$relLabel`{`${CypherStatement.COMPOSITE_PROPERTY}`:true}]->(tgt)
        """
        //return "MERGE (:`$fromLabel`{`${CypherStatement.PATH_PROPERTY}`:'$fromId'})-[r:`$relLabel`{`${CypherStatement.COMPOSITE_PROPERTY}`:true}]->(:`$toLabel`{`${CypherStatement.PATH_PROPERTY}`:'$toId'})"
    }
}

data class CypherProperty(val name: String, val value: CypherValue) {
    fun toCypherString(): String = "`${name}`: ${value.toCypherString()}"
}

data class CypherObject(
        override val label: String,
        override val path: String
) : CypherElement {

    val properties = mutableListOf<CypherProperty>()

    override fun toCypherStatement(): String {
        val propertyStr = this.properties.filter { null != it.value.value }.map { it.toCypherString() }.joinToString(", ")
        return if (propertyStr.isEmpty()) {
            "MERGE (:`$label`{`${CypherStatement.PATH_PROPERTY}`:'$path'})"
        } else {
            "MERGE (:`$label`{`${CypherStatement.PATH_PROPERTY}`:'$path', $propertyStr})"
        }
    }
}

data class CypherMatchNode(
        val label: String,
        val nodeName: String
) : CypherStatement {
    val properties = mutableListOf<CypherProperty>()
    override fun toCypherStatement(): String {
        val propertyStr = this.properties.map { it.toCypherString() }.joinToString(", ")
        return if (propertyStr.isEmpty()) {
            "MATCH (`$nodeName`:`$label`) RETURN `$nodeName`"
        } else {
            "MATCH (`$nodeName`:`$label`{$propertyStr}) RETURN `$nodeName`"
        }
    }

    override fun toString(): String {
        return this.toCypherStatement()
    }
}

data class CypherMatchLink(
        val srcLabel:String,
        val srcNodeName:String,
        val lnkLabel:String,
        val tgtLabel:String,
        val tgtNodeName:String
) : CypherStatement {
    override fun toCypherStatement(): String {
        return "MATCH (`$srcNodeName`:`$srcLabel`)-[:`$lnkLabel`]-(`$tgtNodeName`:`$tgtLabel`) RETURN `$srcNodeName`, `$tgtNodeName`"
    }

    override fun toString(): String {
        return this.toCypherStatement()
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

    private fun <T : Any> createCypherMergeStatements(datatype: Datatype, rootItem: T): List<CypherStatement> {
        val idProps = datatype.identityProperties
        if (idProps.size != 1) {
            throw PersistenceException("Currently a root item must have 1 identity property")
        }
        val rootIdentity = idProps[0].get(rootItem).toString()

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
                val objId = path.joinToString("/", "/$rootIdentity/")
                val stm = when {
                    type.isList -> CypherList(objId)
                    type.isSet -> CypherSet(objId)
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
                val objId = path.joinToString("/", "/$rootIdentity/")
                val stm = CypherMap(objId)
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
                val entryId = path.joinToString("/", "/$rootIdentity/")
                val cyEntry = CypherMapEntry(entryId)
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
                val objId = path.joinToString("/", "/$rootIdentity/")
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

    private fun createCypherMatchObject(objPathName:String, objDatatype: Datatype): List<CypherStatement> {
        val objLabel = objDatatype.qualifiedName(".")
        //TODO: handle composition and reference!
        val cypherStatement = CypherMatchNode(objLabel, objPathName)
        val composite = objDatatype.property.values.filter {
            it.propertyType.isPrimitive.not()
        }.map {
            val childLabel = it.propertyType.qualifiedName(".")
            val childNodeName = objPathName + "/${it.name}"
            CypherMatchLink(objLabel, objPathName, it.name, childLabel, childNodeName)
        }
        return listOf(cypherStatement)
    }

    private fun createMatchSet(path:String) : List<CypherStatement>{
        val set = CypherMatchNode(CypherStatement.MAP_TYPE_LABEL, path)
        val elements = CypherMatchLink(set.label, set.nodeName, CypherStatement.ELEMENT_RELATION, "?", "element")
        return listOf(set) + elements
    }
    private fun createMatchList(path:String) : List<CypherStatement>{
        val list = CypherMatchNode(CypherStatement.MAP_TYPE_LABEL, path)
        val elements = CypherMatchLink(list.label, list.nodeName, CypherStatement.ELEMENT_RELATION, "?", "element")
        return listOf(list) + elements
    }
    private fun createMatchMap(path:String) : List<CypherStatement>{
        val map = CypherMatchNode(CypherStatement.MAP_TYPE_LABEL, path)
        val entries = CypherMatchNode(CypherStatement.MAPENTRY_TYPE_LABEL, "$path/entry")
        return listOf(map) + entries
    }

    private fun createCypherMatchRootObject(datatype: Datatype, filterSet: Set<Filter>): List<CypherStatement> {
        val rootLabel = datatype.qualifiedName(".")
        val rootNodeName = "/" + (filterSet.first() as FilterProperty).value as String
        //TODO: handle composition and reference!
        val cypherStatement = CypherMatchNode(rootLabel, rootNodeName)
        filterSet.forEach { filter ->
            when (filter) {
                is FilterProperty -> {
                    cypherStatement.properties.add(CypherProperty(filter.propertyName, CypherValue(filter.value)))
                }
                else -> throw PersistenceException("Filter type ${filter::class.simpleName} is not yet supported")
            }
        }

        val composite = datatype.property.values.filter {
            it.propertyType.isPrimitive.not()
        }.flatMap {
            val ppath = rootNodeName + "/${it.name}"
            val pt =  it.propertyType
            when {
                pt.isCollection -> {
                    val pct = pt as CollectionType
                    when {
                        pct.isSet-> createMatchSet(ppath)
                        pct.isList-> createMatchList(ppath)
                        pct.isMap -> createMatchMap(ppath)
                        else -> throw PersistenceException("unsupported collection type ${pct.qualifiedName(".")}")
                    }
                }
                else -> {
                    val childLabel = pt.qualifiedName(".")
                    // CypherMatchLink(rootLabel, rootNodeName, it.name, childLabel, childNodeName)
                    val match = CypherMatchNode(childLabel, ppath)
                    match.properties.add(CypherProperty(CypherStatement.PATH_PROPERTY, CypherValue(ppath)))
                    listOf(match)
                }
            }
        }
        return listOf(cypherStatement) + composite
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

    private fun executeReadCypher(cypherStatements: List<CypherStatement>): List<Record> {
        //TODO: use 'USING PERIODIC COMMIT' to improve performance
        val records = mutableListOf<Record>()
        this._neo4j.session().use { session ->
            session.readTransaction { tx ->
                cypherStatements.forEach { stm ->
                    val cypherStr = stm.toCypherStatement()
                    af.log.trace { "executeReadCypher($cypherStr)" }
                    val result = tx.run(cypherStr)
                    records.addAll(result.list())
                }
            }
        }
        return records
    }

    private fun recordsToPathMap(records:List<Record>) : Map<String,Value> {
        return records.associate{ val key = it.keys().first(); Pair(key, it[key]) }
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
            ts.NODE() -> {
                val node = neo4jValue.asNode()
                when {
                    node.hasLabel(CypherStatement.SET_TYPE_LABEL) -> convertSetNode()
                    node.hasLabel(CypherStatement.LIST_TYPE_LABEL) -> convertListNode()
                    node.hasLabel(CypherStatement.MAP_TYPE_LABEL) -> convertMapNode()
                    else -> TODO()
                }
            }
            else -> throw PersistenceException("Neo4j value type ${neo4jValue.type()} is not yet supported")
        }
    }

    fun convertSetNode() : Set<Any> {
        return emptySet()
    }

    fun convertListNode() : List<Any> {
        return emptyList()
    }

    fun convertMapNode() : Map<Any,Any> {
        return emptyMap()
    }

    private fun convertObjects(datatype: Datatype, records: List<Record>, nodeName: String): Set<Any> {
        return records.map {
       //     this.convertObject(datatype, it, nodeName)
        }.toSet()
    }

    private fun convertObject(datatype: Datatype, pathMap: Map<String,Value>, path: String): Any {
        val ts = this._neo4j.session().typeSystem()
        val node = pathMap[path]?.asNode() ?: throw PersistenceException("node $path not found")
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
            val ppath = "$path/${it.name}"
            val neo4jValue = pathMap[ppath]
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
        val dt = this._registry.findDatatypeByClass(type) ?: throw PersistenceException("type ${type.simpleName} is not registered, is the komposite configuration correct")
        val cypherStatements = this.createCypherMergeStatements(dt, item)
        this.executeWriteCypher(cypherStatements)
    }

    override fun <T : Any> createAll(type: KClass<T>, itemSet: Set<T>) {
        af.log.trace { "createAll(${type.simpleName}, ...)" }
        val dt = this._registry.findDatatypeByClass(type) ?: throw PersistenceException("type ${type.simpleName} is not registered, is the komposite configuration correct")
        val cypherStatements = itemSet.flatMap { item ->
            this.createCypherMergeStatements(dt, item)
        }
        this.executeWriteCypher(cypherStatements)
    }

    override fun <T : Any> read(type: KClass<T>, filterSet: Set<Filter>): T {
        af.log.trace { "read(${type.simpleName}, $filterSet)" }
        val dt = this._registry.findDatatypeByClass(type) ?: throw PersistenceException("type ${type.simpleName} is not registered, is the komposite configuration correct")
        val cypherStatements = this.createCypherMatchRootObject(dt, filterSet)
        val records = this.executeReadCypher(cypherStatements)
        val pathMap = recordsToPathMap(records)
        val rootNodeName = records[0].keys().first()
        val item = this.convertObject(dt, pathMap, rootNodeName)
        return item as T
    }

    override fun <T : Any> readAll(type: KClass<T>, filterSet: Set<Filter>): Set<T> {
        af.log.trace { "readAll(${type.simpleName}, $filterSet)" }
        val dt = this._registry.findDatatypeByClass(type) ?: throw PersistenceException("type ${type.simpleName} is not registered, is the komposite configuration correct")
        val cypherStatements = this.createCypherMatchRootObject(dt, filterSet)
        val records = this.executeReadCypher(cypherStatements)
        val rootNodeName = records[0].keys().first() //FIXME
        val itemSet = this.convertObjects(dt, records, rootNodeName)
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