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

import kotlinx.datetime.Instant
import net.akehurst.kaf.technology.persistence.api.PersistenceException
import net.akehurst.kotlinx.komposite.common.DatatypeRegistry
import net.akehurst.kotlinx.komposite.common.PrimitiveMapper
import net.akehurst.language.agl.expressions.processor.constructDataType
import net.akehurst.language.agl.expressions.processor.set
import net.akehurst.language.base.api.asQualifiedName
import net.akehurst.language.typemodel.api.*
import net.akehurst.language.typemodel.asm.StdLibDefault
import org.neo4j.driver.Value
import org.neo4j.driver.types.Node
import org.neo4j.driver.types.Type
import org.neo4j.driver.types.TypeSystem
import kotlin.collections.plus

class FromNeo4JConverter(
    private val reader: Neo4JReader,
    private val ts: TypeSystem,
    private val registry: DatatypeRegistry
) {
    private var pathMap = mutableMapOf<String, Value>()
    private val objectCache = mutableMapOf<String, Any>()

    //TODO: make this fun Value.datatype()
    private fun Type.toDatatype(neo4jValue: Value): TypeInstance = when (this) {
        ts.NULL() -> registry.NothingType.type()
        ts.STRING() -> StdLibDefault.String
        ts.INTEGER() -> StdLibDefault.Integer
        ts.BOOLEAN() -> StdLibDefault.Boolean
        ts.FLOAT() -> StdLibDefault.Real
        ts.LIST() -> StdLibDefault.List.type()
        //ts.SET() -> neo4jValue.asSet()
        ts.MAP() -> StdLibDefault.Map.type()
        ts.DATE_TIME() -> StdLibDefault.Timestamp
        ts.NODE() -> {
            val node = neo4jValue.asNode()
            when {
                node.hasLabel(CypherStatement.SET_TYPE_LABEL) -> StdLibDefault.Set.type()
                node.hasLabel(CypherStatement.LIST_TYPE_LABEL) -> StdLibDefault.List.type()
                node.hasLabel(CypherStatement.MAP_TYPE_LABEL) -> StdLibDefault.Map.type()
                else -> {
                    val className = node[CypherStatement.CLASS_PROPERTY].asString()
                    val classDt = registry.findFirstDefinitionByNameOrNull(className.asQualifiedName.last)
                        ?: error("No datatype information found for $className") //TODO: change when registry supports QualName lookup
                    classDt.type()
                }
            }
        }

        else -> throw PersistenceException("Neo4j value type ${neo4jValue.type()} is not yet supported")
    }

    private fun readSize(stm: CypherStatement): Long {
        val records = this.reader.executeReadCypher(listOf(stm))
        val size = records[0][0].asNode()[CypherStatement.SIZE_PROPERTY].asLong()
        return size
    }

    private fun createCypherMatchItem(path: String, type: TypeInstance) {
        when(type.resolvedDeclaration) {
            is PrimitiveType -> {
            }

            is CollectionType -> when {
                (type.resolvedDeclaration as CollectionType)== StdLibDefault.Set -> createMatchSet(path, type)
                (type.resolvedDeclaration as CollectionType) == StdLibDefault.List -> {
                }

                (type.resolvedDeclaration as CollectionType)==StdLibDefault.Map -> {
                }
            }

            else -> { // isObject

            }
        }
    }

    private fun createCypherMatchRootObject(datatype: DataType, identity: String): List<CypherStatement> {
        //val rootLabel = datatype.qualifiedName
        val rootPath = "/$identity"

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
        //val elementType = type.typeArguments[0]
        val set = CypherMatchNodeByTypeAndPath(CypherStatement.SET_TYPE_LABEL, path)
        return listOf(set)
    }

    private fun createMatchList(path: String, type: TypeInstance): List<CypherStatement> {
        val elementType = type.typeArguments[0]
        return if (elementType.type.resolvedDeclaration is PrimitiveType) {
            val list = CypherMatchNodeByTypeAndPath(CypherStatement.LIST_TYPE_LABEL, path)
            return listOf(list)
        } else {
            val list = CypherMatchList(path, elementType.type.qualifiedTypeName.value)
            listOf(list)
        }
    }

    private fun createMatchMap(path: String, type: TypeInstance): List<CypherStatement> {
        val keyType = type.typeArguments[0]
        val valueType = type.typeArguments[1]
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

    private fun createCypherMatchObject(typeDeclaration: TypeDefinition, objPathName: String): List<CypherStatement> {
        val objLabel = typeDeclaration.qualifiedName
        //TODO: handle composition and reference!
        val cypherStatement = when {
            typeDeclaration == StdLibDefault.AnyType.resolvedDeclaration -> CypherMatchNodeByPath(objPathName)
            else -> CypherMatchNodeByTypeAndPath(objLabel.value, objPathName)
        }
        //cypherStatement.properties.add(CypherProperty(CypherStatement.PATH_PROPERTY, CypherValue(objPathName)))
        val dt = typeDeclaration as DataType
        val composite = dt.property.filter {
            it.isComposite || (it.typeInstance.resolvedDeclaration is PrimitiveType)
        }.flatMap {
            val ppath = objPathName + "/${it.name}"
            val pt = it.typeInstance
            when {
                pt.resolvedDeclaration is PrimitiveType -> {
                    emptyList<CypherStatement>()
                }

                pt.resolvedDeclaration is CollectionType -> {
                    val pct = pt.resolvedDeclaration as CollectionType
                    when {
                        pct == StdLibDefault.Set -> createMatchSet(ppath, pt)
                        pct == StdLibDefault.List -> createMatchList(ppath, pt)
                        pct == StdLibDefault.Map -> createMatchMap(ppath, pt)
                        else -> throw PersistenceException("unsupported collection type ${pct.qualifiedName}")
                    }
                }

                else -> { // isObject
                    val childLabel = pt.resolvedDeclaration.qualifiedName
                    // CypherMatchLink(rootLabel, rootNodeName, it.name, childLabel, childNodeName)
                    val match = CypherMatchNodeByTypeAndPath(childLabel.value, ppath)
                    //match.properties.add(CypherProperty(CypherStatement.PATH_PROPERTY, CypherValue(ppath)))
                    listOf(match) + createCypherMatchObject(pt.resolvedDeclaration, ppath)
                }
            }
        }
        val references = dt.property.filter {
            it.isReference && it.typeInstance.resolvedDeclaration !is PrimitiveType
        }.map {
            //TODO: reference collections !
            CypherMatchReference(
                srcLabel = it.owner.qualifiedName.value,
                srcNodeName = "src",
                lnkLabel = it.name.value,
                lnkName = "rel",
                tgtLabel = it.typeInstance.qualifiedTypeName.value,
                tgtNodeName = "tgt"
            )
        }

        return listOf(cypherStatement) + composite + references
    }

    fun fetchAllIds(datatype: DataType): Set<String> {
        val rootLabel = datatype.qualifiedName
        val key = "n"
        val cypherStatements = listOf(
            CypherMatchAllNodeByType(rootLabel.value, key)
        )
        val records = reader.executeReadCypher(cypherStatements)
        val ids = records.map { rec ->
            rec[key].asNode()[CypherStatement.PATH_PROPERTY].asString().substring(1)
        }.toSet()
        return ids
    }

    fun convertRootObject(datatype: DataType, identity: String): Any {
        val cypherStatements = this.createCypherMatchRootObject(datatype, identity)
        val records = reader.executeReadCypher(cypherStatements)
        if (records.isEmpty()) {
            error("Object with identity '$identity' not found")
        } else {
            this.pathMap = reader.recordsToPathMap(records)
            val rootNodePath = records[0].keys().first()
            val node = pathMap[rootNodePath]?.asNode() ?: throw PersistenceException("node $rootNodePath not found")
            val root = this.convertObject(datatype.type(), node)
            return root
        }
    }

    fun convertPrimitive(type: TypeInstance, raw: Any): Any {
        return when (raw) {
            is String -> {
                val mapper = this.registry.findPrimitiveMapperBySimpleName(type.resolvedDeclaration.name.value)
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
                Instant.fromEpochMilliseconds(unixMillis)
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
        val elementTypeInstance = type.typeArguments[0]
        val path = node[CypherStatement.PATH_PROPERTY].asString()!!
        val size = node[CypherStatement.SIZE_PROPERTY].asInt()
        val set = mutableSetOf<Any?>()
        when {
            elementTypeInstance.type.resolvedDeclaration is PrimitiveType -> {
                if (node.containsKey(CypherStatement.ELEMENTS_PROPERTY)) {
                    val elements = node[CypherStatement.ELEMENTS_PROPERTY].asList()
                    elements.forEach { nEl ->
                        when (nEl) {
                            is Value -> {
                                val el = convertValue(elementTypeInstance.type, nEl)
                                set.add(el)
                            }

                            else -> {
                                val prim = convertPrimitive(elementTypeInstance.type, nEl)
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
                    val cypherValueStatements = this.createCypherMatchObject(elementTypeInstance.type.resolvedDeclaration, elementPath)
                    val res = reader.executeReadCypher(cypherValueStatements) //TODO read all elements at once!
                    val pm = reader.recordsToPathMap(res.toList())
                    pathMap.putAll(pm)
                    val elementNeo4J = pm[elementPath]!!
                    val element = convertValue(elementTypeInstance.type, elementNeo4J)
                    set.add(element)
                }
            }
        }


        return set
    }

    fun convertListNode(type: TypeInstance, node: Node): List<Any?> {
        val elementTypeInstance = type.typeArguments[0]
        val path = node[CypherStatement.PATH_PROPERTY].asString()!!
        val size = node[CypherStatement.SIZE_PROPERTY].asInt()
        val list = mutableListOf<Any?>()
        when {
            elementTypeInstance.type.resolvedDeclaration is PrimitiveType -> {
                if (node.containsKey(CypherStatement.ELEMENTS_PROPERTY)) {
                    val elements = node[CypherStatement.ELEMENTS_PROPERTY].asList()
                    elements.forEach { nEl ->
                        when (nEl) {
                            is Value -> {
                                val el = convertValue(elementTypeInstance.type, nEl)
                                list.add(el)
                            }

                            else -> {
                                val prim = convertPrimitive(elementTypeInstance.type, nEl)
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
                    val elementPath = "$path/${CypherStatement.ELEMENT_PATH_SEGMENT}/$elementIndex"
                    val cypherValueStatements = this.createCypherMatchObject(elementTypeInstance.type.resolvedDeclaration, "$elementPath")
                    val res = reader.executeReadCypher(cypherValueStatements) //TODO read all elements at once!
                    val pm = reader.recordsToPathMap(res.toList())
                    pathMap.putAll(pm)
                    val elementNeo4J = pm["$elementPath"]!!
                    val element = convertValue(elementTypeInstance.type, elementNeo4J)
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
            val keyType = type.typeArguments[0]
            val key = convertValue(keyType.type, entryNode[CypherStatement.KEY_PROPERTY]) ?: throw PersistenceException("Cannot have a null key")
            val valueType = type.typeArguments[1]
            val cypherValueStatements = this.createCypherMatchObject(valueType.type.resolvedDeclaration, valuePath)
            val res = reader.executeReadCypher(cypherValueStatements)  //TODO read all entries at once!
            val pm = reader.recordsToPathMap(res.toList())
            pathMap.putAll(pm)
            val valueNeo4J = pm[valuePath]!!
            val value = convertValue(valueType.type, valueNeo4J)
            map[key] = value
        }
        return map
    }

    fun convertObject(type: TypeInstance, node: Node): Any {
        if (type.resolvedDeclaration is DataType) {
            val path = node[CypherStatement.PATH_PROPERTY].asString()
            val className = node[CypherStatement.CLASS_PROPERTY].asString()
            return if (objectCache.containsKey(path)) {
                objectCache[path]!!
            } else {
                val classDt = this.registry.findFirstDefinitionByNameOrNull(className.asQualifiedName.last) //TODO: change when registry supports QualName lookup
                if (null == classDt) {
                    throw PersistenceException("No datatype information found for $className")
                } else {
                    /*
                    val idProps = classDt.property.filter { it.isConstructor }.map { prop ->
                        val propPath = when {
                            prop.typeInstance.resolvedDeclaration is PrimitiveType -> "" // not used
                            prop.isReference -> "$path/#ref/${prop.name}"
                            prop.isComposite -> "$path/${prop.name}"
                            else -> error("Cannot calculate property path for '$prop'")
                        }
                        val actualPropType = when {
                            prop.typeInstance.resolvedDeclaration == StdLibDefault.AnyType -> when {
                                node.containsKey(prop.name.value) -> {// try primitive
                                    val neo4jValue = node[prop.name.value]
                                    neo4jValue.type().toDatatype(neo4jValue)
                                }

                                pathMap.containsKey(propPath) -> {
                                    val neo4jValue = pathMap[propPath]!!
                                    neo4jValue.type().toDatatype(neo4jValue)
                                }

                                else -> prop.typeInstance // must be null //error("Cannot calculate actual type of '$prop'")
                            }

                            else -> prop.typeInstance
                        }

                        when {
                            (actualPropType.resolvedDeclaration is PrimitiveType) -> {
                                val neo4JValue = node[prop.name.value]
                                val value = this.convertValue(actualPropType, neo4JValue)
                                value
                            }
                            actualPropType.resolvedDeclaration== StdLibDefault.AnyType -> null// must be null or real type could be figured out above

                            prop.isReference -> { // but not primitive
                                val neo4jValue = pathMap[propPath]
                                if (null != neo4jValue) {
                                    val value = this.convertValue(actualPropType, neo4jValue)
                                    value
                                } else {
                                    null
                                }
                            }

                            prop.isComposite -> { // but not primitive
                                val neo4jValue = pathMap[propPath]
                                if (null != neo4jValue) {
                                    val value = this.convertValue(actualPropType, neo4jValue)
                                    value
                                } else {
                                    null
                                }
                            }

                            else -> throw PersistenceException("Cannot convert ${prop}")
                        }
                    }
                     */
                    val constructorParams = when(classDt) {
                        is DataType -> classDt.constructors[0].parameters
                        is ValueType -> classDt.constructors[0].parameters
                        else -> error("Cannot construct a '${classDt::class.simpleName}' ${classDt.qualifiedName}")
                    }
                    val constructorArgs = constructorParams.map { prop ->
                        val propPath = when {
                            prop.typeInstance.resolvedDeclaration is PrimitiveType -> "" // not used
//                            prop.isReference -> "$path/#ref/${prop.name}"
//                            prop.isComposite -> "$path/${prop.name}"
//                            else -> error("Cannot calculate property path for '$prop'")
                            else -> "$path/${prop.name}"
                        }
                        val actualPropType = when {
                            prop.typeInstance.resolvedDeclaration == StdLibDefault.AnyType -> when {
                                node.containsKey(prop.name.value) -> {// try primitive
                                    val neo4jValue = node[prop.name.value]
                                    neo4jValue.type().toDatatype(neo4jValue)
                                }

                                pathMap.containsKey(propPath) -> {
                                    val neo4jValue = pathMap[propPath]!!
                                    neo4jValue.type().toDatatype(neo4jValue)
                                }

                                else -> prop.typeInstance // must be null //error("Cannot calculate actual type of '$prop'")
                            }

                            else -> prop.typeInstance
                        }
                        when {
                            (actualPropType.resolvedDeclaration is PrimitiveType) -> {
                                val neo4JValue = node[prop.name.value]
                                val value = this.convertValue(actualPropType, neo4JValue)
                                value
                            }
                            actualPropType.resolvedDeclaration== StdLibDefault.AnyType -> null// must be null or real type could be figured out above

                            else -> { // but not primitive
                                val neo4jValue = pathMap[propPath]
                                if (null != neo4jValue) {
                                    val value = this.convertValue(actualPropType, neo4jValue)
                                    value
                                } else {
                                    null
                                }
                            }
                        }
                    }
                    val obj = (classDt as DataType).constructDataType(*constructorArgs.toTypedArray()) //TODO: need better error when this fails
                    objectCache[path] = obj

                    // TODO: change this to enable nonExplicit properties, once JS reflection works
                    classDt.property.filter { it.isReadWrite }.forEach {
                            when {
                                (it.typeInstance.resolvedDeclaration is PrimitiveType) -> {
                                    val neo4JValue = node[it.name.value]
                                    val value = this.convertValue(it.typeInstance, neo4JValue)
                                    it.set(obj, value)
                                }

                                it.isReference -> { // but not primitive
                                    val refPath = "$path/#ref/${it.name}"
                                    val neo4jValue = pathMap[refPath]
                                    if (null != neo4jValue) {
                                        val value = this.convertValue(it.typeInstance, neo4jValue)
                                        it.set(obj, value)
                                    } else {
                                        // do nothing
                                    }

                                }

                                it.isComposite -> { // but not primitive
                                    val ppath = "$path/${it.name}"
                                    val neo4jValue = pathMap[ppath]
                                    if (null != neo4jValue) {
                                        val value = this.convertValue(it.typeInstance, neo4jValue)
                                        it.set(obj, value)
                                    } else {
                                        // do nothing
                                    }
                                }

                                else -> throw PersistenceException("Cannot convert ${it}")
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