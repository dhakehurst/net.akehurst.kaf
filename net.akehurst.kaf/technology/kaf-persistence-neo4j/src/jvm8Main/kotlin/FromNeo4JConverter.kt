package net.akehurst.kaf.technology.persistence.neo4j

import com.soywiz.klock.DateTime
import net.akehurst.kaf.technology.persistence.api.Filter
import net.akehurst.kaf.technology.persistence.api.FilterProperty
import net.akehurst.kaf.technology.persistence.api.PersistenceException
import net.akehurst.kotlin.komposite.api.CollectionType
import net.akehurst.kotlin.komposite.api.Datatype
import net.akehurst.kotlin.komposite.api.TypeDeclaration
import net.akehurst.kotlin.komposite.api.TypeInstance
import net.akehurst.kotlin.komposite.common.construct
import net.akehurst.kotlin.komposite.common.set
import net.akehurst.kotlin.komposite.processor.TypeInstanceSimple
import org.neo4j.driver.v1.Value
import org.neo4j.driver.v1.types.Node
import org.neo4j.driver.v1.types.TypeSystem

class FromNeo4JConverter(
        val reader:Neo4JReader,
        val ts: TypeSystem
) {
    var pathMap = mutableMapOf<String, Value>()

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
            it.propertyType.declaration.isPrimitive.not()
        }.flatMap {
            val ppath = rootNodeName + "/${it.name}"
            val pt = it.propertyType
            when {
                pt.declaration.isCollection -> {
                    val pct = pt.declaration as CollectionType
                    when {
                        pct.isSet -> createMatchSet(ppath)
                        pct.isList -> createMatchList(ppath)
                        pct.isMap -> createMatchMap(ppath)
                        else -> throw PersistenceException("unsupported collection type ${pct.qualifiedName(".")}")
                    }
                }
                else -> {
                    val childLabel = pt.declaration.qualifiedName(".")
                    // CypherMatchLink(rootLabel, rootNodeName, it.name, childLabel, childNodeName)
                    val match = CypherMatchNode(childLabel, ppath)
                    match.properties.add(CypherProperty(CypherStatement.PATH_PROPERTY, CypherValue(ppath)))
                    listOf(match)
                }
            }
        }
        return listOf(cypherStatement) + composite
    }

    private fun createMatchSet(path: String): List<CypherStatement> {
        val set = CypherMatchNode(CypherStatement.MAP_TYPE_LABEL, path)
        val elements = CypherMatchLink(set.label, set.nodeName, CypherStatement.ELEMENT_RELATION, "?", "element")
        return listOf(set) + elements
    }

    private fun createMatchList(path: String): List<CypherStatement> {
        val list = CypherMatchNode(CypherStatement.MAP_TYPE_LABEL, path)
        val elements = CypherMatchLink(list.label, list.nodeName, CypherStatement.ELEMENT_RELATION, "?", "element")
        return listOf(list) + elements
    }

    private fun createMatchMap(path: String): List<CypherStatement> {
        val map = CypherMatchMap(path)
        //val entries = CypherMatchNode(CypherStatement.MAPENTRY_TYPE_LABEL, "$path/${CypherStatement.ENTRY_PATH_SEGMENT}")
        return listOf(map)// + entries
    }

    private fun createCypherMatchObject(typeDeclaration: TypeDeclaration, objPathName: String): List<CypherStatement> {
        val objLabel = typeDeclaration.qualifiedName(".")
        //TODO: handle composition and reference!
        val cypherStatement = CypherMatchNode(objLabel, objPathName)
        cypherStatement.properties.add(CypherProperty(CypherStatement.PATH_PROPERTY,CypherValue(objPathName)))
        val composite = (typeDeclaration as Datatype).property.values.filter {
            it.propertyType.declaration.isPrimitive.not()
        }.map {
            val childLabel = it.propertyType.declaration.qualifiedName(".")
            val childNodeName = objPathName + "/${it.name}"
            CypherMatchLink(objLabel, objPathName, it.name, childLabel, childNodeName)
        }
        return listOf(cypherStatement) + composite
    }

    fun convertRootObject(datatype: Datatype, filterSet: Set<Filter>) :Any {
        val cypherStatements = this.createCypherMatchRootObject(datatype, filterSet)
        val records = reader.executeReadCypher(cypherStatements)
        this.pathMap = reader.recordsToPathMap(records)
        val rootNodePath = records[0].keys().first()
        val node = pathMap[rootNodePath]?.asNode() ?: throw PersistenceException("node $rootNodePath not found")
        val root = this.convertObject(TypeInstanceSimple(datatype, emptyList()), node)
        return root
    }

    fun convertValue(type: TypeInstance, neo4jValue: Value): Any? {
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
                    node.hasLabel(CypherStatement.SET_TYPE_LABEL) -> convertSetNode(type, node)
                    node.hasLabel(CypherStatement.LIST_TYPE_LABEL) -> convertListNode(type, node)
                    node.hasLabel(CypherStatement.MAP_TYPE_LABEL) -> convertMapNode(type, node)
                    else -> convertObject(type, node) //TODO should check cast is valid
                }
            }
            else -> throw PersistenceException("Neo4j value type ${neo4jValue.type()} is not yet supported")
        }
    }

    fun convertSetNode(type: TypeInstance, node: Node): Set<Any> {
        return emptySet()
    }

    fun convertListNode(type: TypeInstance, node: Node): List<Any> {
        return emptyList()
    }

    fun convertMapNode(type: TypeInstance, node: Node): Map<Any, Any?> {
        val path = node[CypherStatement.PATH_PROPERTY].asString()!!
        val size = node[CypherStatement.SIZE_PROPERTY].asInt()

        val map = mutableMapOf<Any, Any?>()
        for (entry in 0 until size) {
            val entryPath = "$path/$entry"
            val entryNode = pathMap[entryPath]!!.asNode()
            val key = convertValue(type, entryNode[CypherStatement.KEY_PROPERTY]) ?: throw PersistenceException("Cannot have a null key")
            val valueType = type.arguments[1]
            val cypherValueStatements = this.createCypherMatchObject(valueType.declaration, "$entryPath/value")
            val res = reader.executeReadCypher(cypherValueStatements)
            val pm = reader.recordsToPathMap(res.toList())
            val valueNeo4J = pm["$entryPath/value"]!!
            val value = convertValue(valueType, valueNeo4J)
            pathMap.putAll(pm)
            map[key] = value
        }
        return map
    }

    fun convertObject(type: TypeInstance, node: Node): Any {
        if (type.declaration is Datatype) {
            val dt = type.declaration as Datatype
            val path = node[CypherStatement.PATH_PROPERTY].asString()
            val idProps = dt.identityProperties.map {
                val neo4jValue = node[it.name]
                //TODO: handel non primitive properties
                if (null == neo4jValue) {
                    null
                } else {
                    val v = this.convertValue(it.propertyType, neo4jValue)
                    v
                }
            }
            val obj = dt.construct(*idProps.toTypedArray()) //TODO: need better error when this fails
            //resolvedReference[path] = obj

            // TODO: change this to enable nonExplicit properties, once JS reflection works
            dt.explicitNonIdentityProperties.forEach {
                val ppath = "$path/${it.name}"
                val neo4jValueList = pathMap[ppath]
                if (null != neo4jValueList) {
                    val value = this.convertValue(it.propertyType, neo4jValueList)
                    it.set(obj, value)
                }
            }
            return obj
        } else {
            throw PersistenceException("type must be a Datatype to convert to an object")
        }
    }
}