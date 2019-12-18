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

import net.akehurst.kaf.technology.persistence.api.PersistenceException
import java.time.ZonedDateTime

interface CypherStatement {
    companion object {
        val PATH_PROPERTY = "#path"
        val COMPOSITE_PROPERTY = "#isComposite"
        val SIZE_PROPERTY = "#size"
        val ELEMENTS_PROPERTY = "#elements"
        val ELEMENT_RELATION = "#element"
        val SET_TYPE_LABEL = "#SET"
        val LIST_TYPE_LABEL = "#LIST"

        val MAP_TYPE_LABEL = "#MAP"
        val ENTRY_RELATION = "#entry"
        val MAPENTRY_TYPE_LABEL = "#MAPENTRY"
        val KEY_PROPERTY = "#key"
        val VALUE_PROPERTY = "#value"
        val KEY_RELATION = "#key"
        val VALUE_RELATION = "#value"

        val ENTRY_PATH_SEGMENT = "#entry"
        val ELEMENT_PATH_SEGMENT = "#element"
        val RAW_VALUE = "#RAW"
    }

    fun toCypherStatement(): String
}

interface CypherElement : CypherStatement {
    val label: String
    val path: String
}

data class CypherValue(
        val value: Any?,
        val primitiveTypeName:String? = null
) {
    fun toCypherString(): String {
        return if (null==primitiveTypeName) {
            when {
                null == value -> "NULL"
                value is String -> "'${CypherStatement.RAW_VALUE}|$value'"
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
                value is ZonedDateTime -> "datetime('${value}')"
                else -> throw PersistenceException("CypherValue of type ${value::class.simpleName} is not yet supported")
            }
        } else {
            when {
                null == value -> "NULL"
                value is String -> "'$primitiveTypeName|$value'"
                else -> throw PersistenceException("CypherValue for $primitiveTypeName of type ${value::class.simpleName} is not yet supported")
            }
        }
    }
}

data class CypherList(
        override val path: String,
        val size: Int
) : CypherElement {

    private val _primitiveElements = mutableListOf<CypherValue>()

    val isPrimitiveCollection get() = this._primitiveElements.isNotEmpty()

    override val label = CypherStatement.LIST_TYPE_LABEL

    fun addPrimitiveElement(element: CypherValue) = this._primitiveElements.add(element)

    override fun toCypherStatement(): String {
        return if (this.isPrimitiveCollection) {
            val elements = this._primitiveElements.map { it.toCypherString() }.joinToString(separator = ",", prefix = "[", postfix = "]")
            "MERGE (:`$label`{`${CypherStatement.PATH_PROPERTY}`:'$path', `${CypherStatement.SIZE_PROPERTY}`:$size, `${CypherStatement.ELEMENTS_PROPERTY}`:$elements})"
        } else {
            "MERGE (:`$label`{`${CypherStatement.PATH_PROPERTY}`:'$path', `${CypherStatement.SIZE_PROPERTY}`:$size})"
        }
    }
}

data class CypherSet(
        override val path: String,
        val size: Int
) : CypherElement {

    private val _primitiveElements = mutableListOf<CypherValue>()

    val isPrimitiveCollection get() = this._primitiveElements.isNotEmpty()

    override val label = CypherStatement.SET_TYPE_LABEL

    fun addPrimitiveElement(element: CypherValue) = this._primitiveElements.add(element)

    override fun toCypherStatement(): String {
        return if (this.isPrimitiveCollection) {
            val elements = this._primitiveElements.map { it.toCypherString() }.joinToString(separator = ",", prefix = "[", postfix = "]")
            "MERGE (:`$label`{`${CypherStatement.PATH_PROPERTY}`:'$path', `${CypherStatement.SIZE_PROPERTY}`:$size, `${CypherStatement.ELEMENTS_PROPERTY}`:$elements})"
        } else {
            "MERGE (:`$label`{`${CypherStatement.PATH_PROPERTY}`:'$path', `${CypherStatement.SIZE_PROPERTY}`:$size})"
        }
    }
}

data class CypherMap(
        override val path: String,
        val size: Int
) : CypherElement {

    override val label = CypherStatement.MAP_TYPE_LABEL

    override fun toCypherStatement(): String {
        return "MERGE (:`$label`{`${CypherStatement.PATH_PROPERTY}`:'$path', `${CypherStatement.SIZE_PROPERTY}`:$size})"
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
        override val path: String,
        val additionalLabels : List<String> = emptyList()
) : CypherElement {

    val properties = mutableListOf<CypherProperty>()

    override fun toCypherStatement(): String {
        val labelList = (listOf(label)+additionalLabels).map { "`$it`" }.joinToString(":")
        val propertyStr = this.properties.filter { null != it.value.value }.map { it.toCypherString() }.joinToString(", ")
        return if (propertyStr.isEmpty()) {
            "MERGE (:$labelList{`${CypherStatement.PATH_PROPERTY}`:'$path'})"
        } else {
            "MERGE (:$labelList{`${CypherStatement.PATH_PROPERTY}`:'$path', $propertyStr})"
        }
    }
}

data class CypherMatchAllNodeByType(
        val label: String,
        val key:String
) : CypherStatement {
    val properties = mutableListOf<CypherProperty>()
    override fun toCypherStatement(): String {
        val propertyStr = this.properties.map { it.toCypherString() }.joinToString(", ")
        return if (propertyStr.isEmpty()) {
            "MATCH (n:`$label`) RETURN `$key`"
        } else {
            "MATCH (n:`$label`{$propertyStr}) RETURN `$key`"
        }
    }

    override fun toString(): String {
        return this.toCypherStatement()
    }
}

data class CypherMatchNodeByTypeAndPath(
        val label: String,
        val path: String
) : CypherStatement {
    val properties = mutableListOf<CypherProperty>()
    override fun toCypherStatement(): String {
        val propertyStr = this.properties.map { it.toCypherString() }.joinToString(", ")
        return if (propertyStr.isEmpty()) {
            "MATCH (`$path`:`$label`{`${CypherStatement.PATH_PROPERTY}`:'$path'}) RETURN `$path`"
        } else {
            "MATCH (`$path`:`$label`{`${CypherStatement.PATH_PROPERTY}`:'$path',$propertyStr}) RETURN `$path`"
        }
    }

    override fun toString(): String {
        return this.toCypherStatement()
    }
}

data class CypherMatchList(
        val path: String,
        val elementTypeLabel: String
): CypherStatement {
    override fun toCypherStatement(): String {
        return """
            MATCH (`$path`:`${CypherStatement.LIST_TYPE_LABEL}`{`${CypherStatement.PATH_PROPERTY}`:`$path`})
            UNWIND(range(0,`$path`.`${CypherStatement.SIZE_PROPERTY}`-1)) AS elementIndex
            MATCH (`$path/${CypherStatement.ELEMENT_PATH_SEGMENT}`:`${elementTypeLabel}`) WHERE `$path/${CypherStatement.ELEMENT_PATH_SEGMENT}`.`${CypherStatement.PATH_PROPERTY}`='$path/'+elementIndex
            RETURN `$path`, `$path/${CypherStatement.ELEMENT_PATH_SEGMENT}`, '$path/'+elementIndex AS elementPath
        """.trimIndent()
    }
}

data class CypherMatchMap(
        val path: String
): CypherStatement {
    override fun toCypherStatement(): String {
        return """
            MATCH (`$path`:`${CypherStatement.MAP_TYPE_LABEL}`)
            UNWIND(range(0,`$path`.`${CypherStatement.SIZE_PROPERTY}`-1)) AS entryIndex
            MATCH (`$path/${CypherStatement.ENTRY_PATH_SEGMENT}`:`${CypherStatement.MAPENTRY_TYPE_LABEL}`) WHERE `$path/${CypherStatement.ENTRY_PATH_SEGMENT}`.`${CypherStatement.PATH_PROPERTY}`='$path/'+entryIndex
            RETURN `$path`, `$path/${CypherStatement.ENTRY_PATH_SEGMENT}`, '$path/'+entryIndex AS entryPath
        """.trimIndent()
    }
}

data class CypherMatchLink(
        val srcLabel: String,
        val srcNodeName: String,
        val lnkLabel: String,
        val tgtLabel: String,
        val tgtNodeName: String
) : CypherStatement {
    override fun toCypherStatement(): String {
        return "MATCH (`$srcNodeName`:`$srcLabel`)-[:`$lnkLabel`]-(`$tgtNodeName`:`$tgtLabel`) RETURN `$srcNodeName`, `$tgtNodeName`"
    }

    override fun toString(): String {
        return this.toCypherStatement()
    }
}

data class CypherDeleteRecursive(
        val label: String,
        val path: String
) : CypherStatement {
    override fun toCypherStatement(): String {
        return "match (`$path`:`$label`{`${CypherStatement.PATH_PROPERTY}`:'$path'})-[r*0..]->(m) detach delete `$path`,m"
    }

    override fun toString(): String {
        return super.toString()
    }
}