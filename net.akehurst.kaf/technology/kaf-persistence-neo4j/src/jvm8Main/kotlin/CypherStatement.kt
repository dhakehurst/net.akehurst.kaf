package net.akehurst.kaf.technology.persistence.neo4j

import com.soywiz.klock.DateTime
import net.akehurst.kaf.technology.persistence.api.PersistenceException

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
    var size = 0

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
