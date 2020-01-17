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
import net.akehurst.kaf.technology.persistence.api.PersistenceException
import net.akehurst.kotlin.komposite.api.*
import net.akehurst.kotlin.komposite.common.DatatypeRegistry
import net.akehurst.kotlin.komposite.common.construct
import net.akehurst.kotlin.komposite.common.set
import net.akehurst.kotlin.komposite.processor.TypeInstanceSimple
import org.neo4j.driver.Value
import org.neo4j.driver.types.Node
import org.neo4j.driver.types.TypeSystem

class FromNeo4JConverter(
        val reader: Neo4JReader,
        val ts: TypeSystem,
        val registry: DatatypeRegistry
) {
    var pathMap = mutableMapOf<String, Value>()
    val objectCache = mutableMapOf<String, Any>()

    private fun readSize(stm: CypherStatement): Long {
        val records = this.reader.executeReadCypher(listOf(stm))
        val size = records[0][0].asNode()[CypherStatement.SIZE_PROPERTY].asLong()
        return size
    }

    private fun createCypherMatchItem(path: String, type: TypeInstance) {
        when {
            type.declaration.isPrimitive -> {
            }
            type.declaration.isCollection -> when {
                (type.declaration as CollectionType).isArray -> {
                }
                (type.declaration as CollectionType).isSet -> createMatchSet(path, type)
                (type.declaration as CollectionType).isList -> {
                }
                (type.declaration as CollectionType).isMap -> {
                }
            }
            else -> { // isObject

            }
        }
    }

    private fun createCypherMatchRootObject(datatype: Datatype, identity: String): List<CypherStatement> {
        val rootLabel = datatype.qualifiedName(".")
        val rootPath = "/" + identity

        val stms = createCypherMatchObject(datatype, rootPath)
        return stms
        /*
        //TODO: handle composition and reference!
        val cypherStatement = CypherMatchNodeByTypeAndPath(rootLabel, rootNodeName)

        val composite = datatype.allExplicitProperty.values.filter {
            it.propertyType.declaration.isPrimitive.not()
        }.flatMap {
            val ppath = rootNodeName + "/${it.name}"
            val pt = it.propertyType
            when {
                pt.declaration.isCollection -> {
                    val pct = pt.declaration as CollectionType
                    when {
                        pct.isSet -> createMatchSet(ppath, pt)
                        pct.isList -> createMatchList(ppath, pt)
                        pct.isMap -> createMatchMap(ppath, pt)
                        else -> throw PersistenceException("unsupported collection type ${pct.qualifiedName(".")}")
                    }
                }
                else -> {
                    val childLabel = pt.declaration.qualifiedName(".")
                    // CypherMatchLink(rootLabel, rootNodeName, it.name, childLabel, childNodeName)
                    val match = CypherMatchNodeByTypeAndPath(childLabel, ppath)
                    match.properties.add(CypherProperty(CypherStatement.PATH_PROPERTY, CypherValue(ppath)))
                    listOf(match)
                }
            }
        }
        return listOf(cypherStatement) + composite
         */
    }

    private fun createMatchSet(path: String, type: TypeInstance): List<CypherStatement> {
        val elementType = type.arguments[0]
        val set = CypherMatchNodeByTypeAndPath(CypherStatement.SET_TYPE_LABEL, path)
        return listOf(set)
    }

    private fun createMatchList(path: String, type: TypeInstance): List<CypherStatement> {
        val elementType = type.arguments[0]
        return if (elementType.declaration.isPrimitive) {
            val list = CypherMatchNodeByTypeAndPath(CypherStatement.LIST_TYPE_LABEL, path)
            return listOf(list)
        } else {
            val list = CypherMatchList(path, elementType.declaration.qualifiedName("."))
            listOf(list)
        }
    }

    private fun createMatchMap(path: String, type: TypeInstance): List<CypherStatement> {
        val keyType = type.arguments[0]
        val valueType = type.arguments[1]
        val map = CypherMatchMap(path)
        /*
        val size = this.readSize(map)
        val entries = mutableListOf<CypherStatement>()
        for (i in 0..size - 1) {
            val entry = CypherMatchNodeByTypeAndPath(CypherStatement.MAPENTRY_TYPE_LABEL, "$path/${CypherStatement.ENTRY_PATH_SEGMENT}/$i")
            entries.add(entry)

        }
         */
        return listOf(map) //+ entries
    }

    private fun createCypherMatchObject(typeDeclaration: TypeDeclaration, objPathName: String): List<CypherStatement> {
        val objLabel = typeDeclaration.qualifiedName(".")
        //TODO: handle composition and reference!
        val cypherStatement = CypherMatchNodeByTypeAndPath(objLabel, objPathName)
        //cypherStatement.properties.add(CypherProperty(CypherStatement.PATH_PROPERTY, CypherValue(objPathName)))
        val composite = (typeDeclaration as Datatype).allExplicitProperty.values.filter {
            it.isComposite or it.propertyType.declaration.isPrimitive
        }.flatMap {
            val ppath = objPathName + "/${it.name}"
            val pt = it.propertyType
            when {
                pt.declaration.isPrimitive -> {
                    emptyList<CypherStatement>()
                }
                pt.declaration.isCollection -> {
                    val pct = pt.declaration as CollectionType
                    when {
                        pct.isSet -> createMatchSet(ppath, pt)
                        pct.isList -> createMatchList(ppath, pt)
                        pct.isMap -> createMatchMap(ppath, pt)
                        else -> throw PersistenceException("unsupported collection type ${pct.qualifiedName(".")}")
                    }
                }
                else -> { // isObject
                    val childLabel = pt.declaration.qualifiedName(".")
                    // CypherMatchLink(rootLabel, rootNodeName, it.name, childLabel, childNodeName)
                    val match = CypherMatchNodeByTypeAndPath(childLabel, ppath)
                    match.properties.add(CypherProperty(CypherStatement.PATH_PROPERTY, CypherValue(ppath)))
                    listOf(match) + createCypherMatchObject(pt.declaration, ppath)
                }
            }
        }
        val references = (typeDeclaration as Datatype).allExplicitProperty.values.filter {
            it.isReference and it.propertyType.declaration.isPrimitive.not()
        }.map {
            //TODO: reference collections !
            CypherMatchReference(
                    srcLabel = it.datatype.qualifiedName("."),
                    srcNodeName = "src",
                    lnkLabel = it.name,
                    lnkName = "rel",
                    tgtLabel = it.propertyType.declaration.qualifiedName("."),
                    tgtNodeName = "tgt"
            )
        }

        return listOf(cypherStatement) + composite + references
    }

    fun fetchAllIds(datatype: Datatype): Set<String> {
        val rootLabel = datatype.qualifiedName(".")
        val key = "n"
        val cypherStatements = listOf(
                CypherMatchAllNodeByType(rootLabel, key)
        )
        val records = reader.executeReadCypher(cypherStatements)
        val ids = records.map { rec ->
            rec[key].asNode()[CypherStatement.PATH_PROPERTY].asString().substring(1)
        }.toSet()
        return ids
    }

    fun convertRootObject(datatype: Datatype, identity: String): Any {
        val cypherStatements = this.createCypherMatchRootObject(datatype, identity)
        val records = reader.executeReadCypher(cypherStatements)
        this.pathMap = reader.recordsToPathMap(records)
        val rootNodePath = records[0].keys().first()
        val node = pathMap[rootNodePath]?.asNode() ?: throw PersistenceException("node $rootNodePath not found")
        val root = this.convertObject(TypeInstanceSimple(datatype, emptyList()), node)
        return root
    }

    fun convertPrimitive(type: TypeInstance, raw: Any): Any {
        return when (raw) {
            is String -> {
                val mapper = this.registry.findPrimitiveMapperFor(type.declaration.name)
                if (null == mapper) {
                    raw
                } else {
                    (mapper as PrimitiveMapper<Any, String>).toPrimitive(raw)
                }
            }
            else -> raw
        }
    }

    fun convertValue(type: TypeInstance, neo4jValue: Value): Any? {
        return when (neo4jValue.type()) {
            ts.NULL() -> null
            ts.STRING() -> convertPrimitive(type, neo4jValue.asString())
            ts.INTEGER() -> neo4jValue.asInt()
            ts.BOOLEAN() -> neo4jValue.asBoolean()
            ts.FLOAT() -> neo4jValue.asDouble()
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

    fun convertSetNode(type: TypeInstance, node: Node): Set<Any?> {
        val elementTypeInstance = type.arguments[0]
        val path = node[CypherStatement.PATH_PROPERTY].asString()!!
        val size = node[CypherStatement.SIZE_PROPERTY].asInt()
        val set = mutableSetOf<Any?>()
        when {
            elementTypeInstance.declaration.isPrimitive -> {
                if (node.containsKey(CypherStatement.ELEMENTS_PROPERTY)) {
                    val elements = node[CypherStatement.ELEMENTS_PROPERTY].asList()
                    elements.forEach { nEl ->
                        when (nEl) {
                            is Value -> {
                                val el = convertValue(elementTypeInstance, nEl)
                                set.add(el)
                            }
                            else -> {
                                val prim = convertPrimitive(elementTypeInstance, nEl)
                                set.add(prim)
                            }
                        }
                    }
                } else {
                    // no elements
                }
            }
            else -> {
                for (elementIndex in 0 until size) {
                    val elementPath = "$path/${CypherStatement.ELEMENT_PATH_SEGMENT}/$elementIndex"
                    val cypherValueStatements = this.createCypherMatchObject(elementTypeInstance.declaration, "$elementPath")
                    val res = reader.executeReadCypher(cypherValueStatements) //TODO read all elements at once!
                    val pm = reader.recordsToPathMap(res.toList())
                    pathMap.putAll(pm)
                    val elementNeo4J = pm["$elementPath"]!!
                    val element = convertValue(elementTypeInstance, elementNeo4J)
                    set.add(element)
                }
            }
        }


        return set
    }

    fun convertListNode(type: TypeInstance, node: Node): List<Any?> {
        val elementTypeInstance = type.arguments[0]
        val path = node[CypherStatement.PATH_PROPERTY].asString()!!
        val size = node[CypherStatement.SIZE_PROPERTY].asInt()
        val list = mutableListOf<Any?>()
        when {
            elementTypeInstance.declaration.isPrimitive -> {
                if (node.containsKey(CypherStatement.ELEMENTS_PROPERTY)) {
                    val elements = node[CypherStatement.ELEMENTS_PROPERTY].asList()
                    elements.forEach { nEl ->
                        when (nEl) {
                            is Value -> {
                                val el = convertValue(elementTypeInstance, nEl)
                                list.add(el)
                            }
                            else -> {
                                val prim = convertPrimitive(elementTypeInstance, nEl)
                                list.add(prim)
                            }
                        }
                    }
                } else {
                    // no elements
                }
            }
            else -> {
                for (elementIndex in 0 until size) {
                    val elementPath = "$path/$elementIndex"
                    val cypherValueStatements = this.createCypherMatchObject(elementTypeInstance.declaration, "$elementPath")
                    val res = reader.executeReadCypher(cypherValueStatements) //TODO read all elements at once!
                    val pm = reader.recordsToPathMap(res.toList())
                    pathMap.putAll(pm)
                    val elementNeo4J = pm["$elementPath"]!!
                    val element = convertValue(elementTypeInstance, elementNeo4J)
                    list.add(element)
                }
            }
        }


        return list
    }

    fun convertMapNode(type: TypeInstance, node: Node): Map<Any, Any?> {
        val path = node[CypherStatement.PATH_PROPERTY].asString()!!
        val size = node[CypherStatement.SIZE_PROPERTY].asInt()

        val map = mutableMapOf<Any, Any?>()
        for (entry in 0 until size) {
            val entryPath = "$path/${CypherStatement.ENTRY_PATH_SEGMENT}/$entry"
            val valuePath = "$entryPath/${CypherStatement.VALUE_PATH_SEGMENT}"
            val entryNode = pathMap[entryPath]!!.asNode()
            val keyType = type.arguments[0]
            val key = convertValue(keyType, entryNode[CypherStatement.KEY_PROPERTY]) ?: throw PersistenceException("Cannot have a null key")
            val valueType = type.arguments[1]
            val cypherValueStatements = this.createCypherMatchObject(valueType.declaration, valuePath)
            val res = reader.executeReadCypher(cypherValueStatements)  //TODO read all entries at once!
            val pm = reader.recordsToPathMap(res.toList())
            pathMap.putAll(pm)
            val valueNeo4J = pm[valuePath]!!
            val value = convertValue(valueType, valueNeo4J)
            map[key] = value
        }
        return map
    }

    fun convertObject(type: TypeInstance, node: Node): Any {
        if (type.declaration is Datatype) {
            val path = node[CypherStatement.PATH_PROPERTY].asString()
            val className = node[CypherStatement.CLASS_PROPERTY].asString()
            return if (objectCache.containsKey(path)) {
                objectCache[path]!!
            } else {
                val classDt = this.registry.findDatatypeByName(className.substringAfterLast(".")) //TODO: change when registry supports QualName lookup
                if (null == classDt) {
                    throw PersistenceException("No datatype information found for $className")
                } else {
                    val idProps = classDt.identityProperties.map { prop ->
                        when {
                            (prop.propertyType.declaration.isPrimitive) -> {
                                val neo4JValue = node[prop.name]
                                val value = this.convertValue(prop.propertyType, neo4JValue)
                                value
                            }
                            prop.isReference -> { // but not primitive
                                val refPath = "$path/#ref/${prop.name}"
                                val neo4jValue = pathMap[refPath]
                                if (null != neo4jValue) {
                                    val value = this.convertValue(prop.propertyType, neo4jValue)
                                    value
                                } else {
                                    null
                                }
                            }
                            prop.isComposite -> { // but not primitive
                                val ppath = "$path/${prop.name}"
                                val neo4jValue = pathMap[ppath]
                                if (null != neo4jValue) {
                                    val value = this.convertValue(prop.propertyType, neo4jValue)
                                    value
                                } else {
                                    null
                                }
                            }
                            else -> throw PersistenceException("Cannot convert ${prop}")
                        }
                    }
                    val obj = classDt.construct(*idProps.toTypedArray()) //TODO: need better error when this fails
                    objectCache[path] = obj

                    // TODO: change this to enable nonExplicit properties, once JS reflection works
                    classDt.allExplicitNonIdentityProperties.forEach {
                        if (it.ignore.not()) {
                            when {
                                (it.propertyType.declaration.isPrimitive) -> {
                                    val neo4JValue = node[it.name]
                                    val value = this.convertValue(it.propertyType, neo4JValue)
                                    it.set(obj, value)
                                }
                                it.isReference -> { // but not primitive
                                    val refPath = "$path/#ref/${it.name}"
                                    val neo4jValue = pathMap[refPath]
                                    if (null != neo4jValue) {
                                        val value = this.convertValue(it.propertyType, neo4jValue)
                                        it.set(obj, value)
                                    } else {
                                        // do nothing
                                    }

                                }
                                it.isComposite -> { // but not primitive
                                    val ppath = "$path/${it.name}"
                                    val neo4jValue = pathMap[ppath]
                                    if (null != neo4jValue) {
                                        val value = this.convertValue(it.propertyType, neo4jValue)
                                        it.set(obj, value)
                                    } else {
                                        // do nothing
                                    }
                                }
                                else -> throw PersistenceException("Cannot convert ${it}")
                            }
                        }
                    }
                    obj
                }
            }
        } else {
            throw PersistenceException("type must be a Datatype to convert to an object")
        }
    }

    private fun setPrimitive() {

    }
}