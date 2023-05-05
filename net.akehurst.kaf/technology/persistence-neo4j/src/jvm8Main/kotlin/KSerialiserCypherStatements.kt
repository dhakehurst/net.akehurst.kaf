package net.akehurst.kaf.technology.persistence.neo4j

import net.akehurst.kaf.technology.persistence.api.PersistenceException
import net.akehurst.kotlin.komposite.api.PrimitiveMapper
import net.akehurst.kotlin.komposite.common.DatatypeRegistry
import net.akehurst.kotlin.komposite.common.WalkInfo
import net.akehurst.kotlin.komposite.common.kompositeWalker
import net.akehurst.kotlinx.collections.Stack

class KSerialiserCypherStatements(
        val registry: DatatypeRegistry
) {

    class FoundReferenceException : RuntimeException {
        constructor() : super()
    }

    internal val reference_cache = mutableMapOf<Any, List<String>>()

    protected fun calcReferencePath(root: Any, targetValue: Any): List<String> {
        return if (reference_cache.containsKey(targetValue)) {
            reference_cache[targetValue]!!
        } else {
            var resultPath: List<String>? = null //TODO: terminate walking early if result found
            val walker = kompositeWalker<List<String>, Boolean>(registry) {
                configure {
                    ELEMENTS = CypherStatement.ELEMENT_RELATION
                    ENTRIES = CypherStatement.ENTRY_RELATION
                    KEY = CypherStatement.KEY_RELATION
                    VALUE = CypherStatement.VALUE_RELATION
                }
                collBegin { path, info, type, coll ->
                    WalkInfo(info.up, info.acc)
                }
                mapBegin { path, info, map ->
                    WalkInfo(info.up, info.acc)
                }
                //               mapEntryValueBegin { key, info, entry ->
                //                   val path = if (key== KompositeWalker.ROOT) info.path else info.path + key.toString()
                //                   WalkInfo(path, info.acc)
                //               }
                objectBegin { path, info, obj, datatype ->
                    reference_cache[obj] = path
                    if (obj == targetValue) {
                        resultPath = path
                        throw FoundReferenceException()
                        // TODO: find a way to terminate the walk!
                    }
                    WalkInfo(info.up, obj == targetValue)
                }
                propertyBegin { path, info, property ->
                    WalkInfo(info.up, info.acc)
                }
            }

            try {
                val result = walker.walk(WalkInfo(emptyList(), false), root)
            } catch (e: FoundReferenceException) {

            }
            resultPath ?: listOf("${'$'}unknown ${targetValue::class.simpleName}")
        }
    }

    fun <T : Any> createCypherMergeStatements(rootItem: T, identity: T.() -> String): List<CypherStatement> {
        val rootIdentity = rootItem.identity()
        val rootPath = listOf(rootIdentity)
        var currentObjStack = Stack<Any>()
        val walker = kompositeWalker<List<String>, List<CypherStatement>>(this.registry) {
            configure {
                ELEMENTS = CypherStatement.ELEMENT_RELATION
                ENTRIES = CypherStatement.ENTRY_RELATION
                KEY = CypherStatement.KEY_RELATION
                VALUE = CypherStatement.VALUE_RELATION
            }
            nullValue { path, info ->
                currentObjStack.push(CypherValue(null))
                WalkInfo(path, info.acc)
            }
            primitive { path, info, primitive, mapper ->
                val cypherValue = if (null == mapper) {
                    CypherValue(primitive)
                } else {
                    val cy = (mapper as PrimitiveMapper<Any, Any>?)?.toRaw?.invoke(primitive)
                            ?: throw PersistenceException("Do not know how to convert ${primitive::class} to json, did you register its converter")
                    val raw = mapper.toRaw(primitive)
                    CypherValue(raw) //TODO: use qualified name from datatype
                }
                currentObjStack.push(cypherValue)
                WalkInfo(info.up, info.acc)
            }
            reference { path, info, value, property ->
                val refPath = calcReferencePath(rootItem, value)
                val fromLabel = property.datatype.qualifiedName
                val fromId = (rootPath + path.dropLast(1)).joinToString("/", "/")
                val relLabel = path.last()
                val toLabel = property.propertyType.declaration.qualifiedName
                val toId = (rootPath + refPath).joinToString("/", "/")
                val stm = CypherReference(fromLabel, fromId, relLabel, toLabel, toId)
                currentObjStack.push(stm)
                WalkInfo(info.up, info.acc + stm)
            }
            collBegin { path, info, type, coll ->
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
                val objId = (rootPath + path).joinToString("/", "/")
                val additionalLabels = datatype.allSuperTypes.map {
                    it.type.declaration.qualifiedName
                }
                val objClass = datatype.qualifiedName
                val obj = CypherObject(objClass, objId, additionalLabels)
                currentObjStack.push(obj)
                WalkInfo(info.up, info.acc + obj)
            }
            objectEnd { path, info, obj, datatype ->
                WalkInfo(info.up, info.acc)
            }
            propertyBegin { path, info, property ->
                info
            }
            propertyEnd { path, info, property ->
                val key = path.last()
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

}