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

package net.akehurst.hjson

inline fun hjson(documentIdentity: String, init: HJsonDocumentBuilder.() -> Unit): HJsonDocument {
    val builder = HJsonDocumentBuilder(documentIdentity)
    builder.init()
    return builder.build()
}

fun Any.toJson(): HJsonValue {
    return when (this) {
        is Boolean -> HJsonBoolean(this)
        is Number -> HJsonNumber(this.toString())
        is String -> HJsonString(this)
        else -> throw HJsonException("Cannot convert ${this::class.simpleName} to HJsonValue")
    }
}

class HJsonDocumentBuilder(documentIdentity: String) {

    private val _doc = HJsonDocument(documentIdentity)
    private val _rootBuilder = HJsonValueBuilder(_doc, emptyList()) { self ->
        if (null == self.value) {
            //ok
        } else {
            throw HJsonException("There can be only one HJsonValue as a the root of a document")
        }
    }

    fun nullValue() {
        this._rootBuilder.nullValue()
    }

    fun boolean(value: Boolean) {
        this._rootBuilder.boolean(value)
    }

    fun number(value: Number) {
        this._rootBuilder.number(value)
    }

    fun string(value: String) {
        this._rootBuilder.string(value)
    }

    fun primitive(value: Any) {
        this._rootBuilder.primitive(value)
    }

    fun primitiveObject(className: String, value: Any) {
        this._rootBuilder.primitiveObject(className, value)
    }

    fun arrayJson(init: HJsonArrayBuilder.() -> Unit) {
        this._rootBuilder.arrayJson(init)
    }

    fun arrayObject(init: HJsonCollectionBuilder.() -> Unit) {
        this._rootBuilder.arrayObject(init)
    }

    fun listObject(init: HJsonCollectionBuilder.() -> Unit) {
        this._rootBuilder.listObject(init)
    }

    fun setObject(init: HJsonCollectionBuilder.() -> Unit) {
        this._rootBuilder.setObject(init)
    }

    fun mapObject(init: HJsonMapBuilder.() -> Unit) {
        this._rootBuilder.mapObject(init)
    }

    fun objectJson(init: HJsonObjectBuilder.() -> Unit) {
        this._rootBuilder.objectJson(init)
    }

    fun objectReferenceable(className: String, init: HJsonObjectBuilder.() -> Unit) {
        this._rootBuilder.objectReferenceable(className, init)
    }


    fun build(): HJsonDocument {
        this._doc.root = _rootBuilder.value ?: throw HJsonException("The document must have a root value")
        return this._doc
    }
}

class HJsonArrayBuilder(
        val doc: HJsonDocument,
        val path: List<String>
) {

    private val _elements = mutableListOf<HJsonValue>()
    val nextPath: List<String>
        get() {
            return this.path + (this._elements.size + 1).toString()
        }

    fun nullValue() {
        val b = HJsonValueBuilder(doc, nextPath)
        b.nullValue()
        this._elements.add(b.value!!)
    }

    fun boolean(value: Boolean) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.boolean(value)
        this._elements.add(b.value!!)
    }

    fun number(value: Number) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.number(value)
        this._elements.add(b.value!!)
    }

    fun string(value: String) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.string(value)
        this._elements.add(b.value!!)
    }

    fun primitive(value: Any) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.primitive(value)
        this._elements.add(b.value!!)
    }

    fun primitiveObject(className: String, value: Any) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.primitiveObject(className, value)
        this._elements.add(b.value!!)
    }

    fun reference(path:String) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.reference(path)
        this._elements.add(b.value!!)
    }

    fun arrayJson(init: HJsonArrayBuilder.() -> Unit) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.arrayJson(init)
        this._elements.add(b.value!!)
    }

    fun arrayObject(init: HJsonCollectionBuilder.() -> Unit) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.arrayObject(init)
        this._elements.add(b.value!!)
    }

    fun listObject(init: HJsonCollectionBuilder.() -> Unit) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.listObject(init)
        this._elements.add(b.value!!)
    }

    fun setObject(init: HJsonCollectionBuilder.() -> Unit) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.setObject(init)
        this._elements.add(b.value!!)
    }

    fun mapObject(init: HJsonMapBuilder.() -> Unit) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.mapObject(init)
        this._elements.add(b.value!!)
    }

    fun objectJson(init: HJsonObjectBuilder.() -> Unit) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.objectJson(init)
        this._elements.add(b.value!!)
    }

    fun objectReferenceable(className: String, init: HJsonObjectBuilder.() -> Unit) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.objectReferenceable(className, init)
        this._elements.add(b.value!!)
    }

    fun build(): HJsonArray {
        val json = HJsonArray()
        json.elements = this._elements
        return json
    }
}

class HJsonCollectionBuilder(
        val doc: HJsonDocument,
        val path: List<String>
) {

    private val _elements = mutableListOf<HJsonValue>()
    val nextPath: List<String>
        get() {
            return this.path + (this._elements.size + 1).toString()
        }

    fun nullValue() {
        val b = HJsonValueBuilder(doc, nextPath)
        b.nullValue()
        this._elements.add(b.value!!)
    }

    fun boolean(value: Boolean) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.boolean(value)
        this._elements.add(b.value!!)
    }

    fun number(value: Number) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.number(value)
        this._elements.add(b.value!!)
    }

    fun string(value: String) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.string(value)
        this._elements.add(b.value!!)
    }

    fun primitive(value: Any) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.primitive(value)
        this._elements.add(b.value!!)
    }

    fun primitiveObject(className: String, value: Any) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.primitiveObject(className, value)
        this._elements.add(b.value!!)
    }

    fun reference(path:String) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.reference(path)
        this._elements.add(b.value!!)
    }

    fun arrayJson(init: HJsonArrayBuilder.() -> Unit) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.arrayJson(init)
        this._elements.add(b.value!!)
    }

    fun arrayObject(init: HJsonCollectionBuilder.() -> Unit) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.arrayObject(init)
        this._elements.add(b.value!!)
    }

    fun listObject(init: HJsonCollectionBuilder.() -> Unit) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.listObject(init)
        this._elements.add(b.value!!)
    }

    fun setObject(init: HJsonCollectionBuilder.() -> Unit) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.setObject(init)
        this._elements.add(b.value!!)
    }

    fun mapObject(init: HJsonMapBuilder.() -> Unit) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.mapObject(init)
        this._elements.add(b.value!!)
    }

    fun objectJson(init: HJsonObjectBuilder.() -> Unit) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.objectJson(init)
        this._elements.add(b.value!!)
    }

    fun objectReferenceable(className: String, init: HJsonObjectBuilder.() -> Unit) {
        val b = HJsonValueBuilder(doc, nextPath)
        b.objectReferenceable(className, init)
        this._elements.add(b.value!!)
    }

    fun build(type: HJsonString): HJsonUnreferencableObject {
        val obj = HJsonUnreferencableObject()
        obj.setProperty(HJsonDocument.TYPE, type)
        val elements = HJsonArray()
        elements.elements = this._elements
        obj.setProperty(HJsonDocument.ELEMENTS, elements)
        return obj
    }
}

class HJsonMapBuilder(
        val doc: HJsonDocument,
        val path: List<String>
) {

    private val _entries = mutableListOf<HJsonUnreferencableObject>()

    fun entry(key: Any, value: Any) {
        val jKey = key.toJson()
        val jValue = value.toJson()
        val entry = HJsonUnreferencableObject()
        entry.setProperty(HJsonDocument.KEY, jKey)
        entry.setProperty(HJsonDocument.VALUE, jValue)
        this._entries.add(entry)
    }

    fun entry(key: HJsonValueBuilder.() -> Unit, value: HJsonValueBuilder.() -> Unit) {
        val kBuilder = HJsonValueBuilder(doc, path) { self ->
            if (null == self.value) {
                //ok
            } else {
                throw HJsonException("There can be only one JsonValue for a map entry key")
            }
        }
        val vBuilder = HJsonValueBuilder(doc, path) { self ->
            if (null == self.value) {
                //ok
            } else {
                throw HJsonException("There can be only one JsonValue for a map entry value")
            }
        }
        kBuilder.key()
        vBuilder.value()
        val jKey = kBuilder.value ?: throw HJsonException("No value for map entry key")
        val jValue = vBuilder.value ?: throw HJsonException("No value for map entry value")
        val entry = HJsonUnreferencableObject()
        entry.setProperty(HJsonDocument.KEY, jKey)
        entry.setProperty(HJsonDocument.VALUE, jValue)
        this._entries.add(entry)
    }

    fun build(): HJsonUnreferencableObject {
        val obj = HJsonUnreferencableObject()
        obj.setProperty(HJsonDocument.TYPE, HJsonDocument.MAP)
        val elements = HJsonArray()
        elements.elements = _entries
        obj.setProperty(HJsonDocument.ELEMENTS, elements)
        return obj
    }
}

class HJsonObjectBuilder(
        val doc: HJsonDocument,
        val path: List<String>
) {

    private val _properties = mutableMapOf<String, HJsonValue>()

    fun property(key: String, value: Any?) {
        val jValue = value?.toJson() ?: HJsonNull
        this._properties[key] = jValue
    }

    fun property(key: String, value: HJsonValueBuilder.() -> Unit) {
        val vBuilder = HJsonValueBuilder(doc, path + key) { self ->
            if (null == self.value) {
                //ok
            } else {
                throw HJsonException("There can be only one JsonValue for an object property")
            }
        }
        vBuilder.value()
        val jValue = vBuilder.value ?: throw HJsonException("No value for object property")
        this._properties[key] = jValue
    }

    fun build(path: List<String>, className: String): HJsonReferencableObject {
        val obj = HJsonReferencableObject(doc, path)
        obj.setProperty(HJsonDocument.TYPE, HJsonDocument.OBJECT)
        obj.setProperty(HJsonDocument.CLASS, HJsonString(className))
        _properties.forEach {
            obj.setProperty(it.key, it.value)
        }
        return obj
    }

    fun build(): HJsonUnreferencableObject {
        val obj = HJsonUnreferencableObject()
        obj.property = _properties
        return obj
    }
}

class HJsonValueBuilder(
        val doc: HJsonDocument,
        val path: List<String>,
        val validate: (self: HJsonValueBuilder) -> Unit = {}
) {

    var value: HJsonValue? = null

    fun nullValue() {
        this.validate(this)
        value = HJsonNull
    }

    fun boolean(value: Boolean) {
        this.validate(this)
        this.value = HJsonBoolean(value)
    }

    fun number(value: Number) {
        this.validate(this)
        this.value = HJsonNumber(value.toString())
    }

    fun string(value: String) {
        this.validate(this)
        this.value = HJsonString(value)
    }

    fun primitive(value: Any) {
        this.validate(this)
        this.value = value.toJson()
    }

    fun primitiveObject(className: String, value: Any) {
        this.validate(this)
        val obj = HJsonUnreferencableObject()
        obj.setProperty(HJsonDocument.TYPE, HJsonDocument.PRIMITIVE)
        obj.setProperty(HJsonDocument.CLASS, HJsonString(className))
        obj.setProperty(HJsonDocument.VALUE, value.toJson())
        this.value = obj
    }

    fun reference(path:String) {
        this.validate(this)
        // remove leading '/' then split
        val refPath = path.substring(1).split("/").toList()
        this.value = HJsonReference(doc,refPath)
    }

    fun arrayJson(init: HJsonArrayBuilder.() -> Unit) {
        this.validate(this)
        val builder = HJsonArrayBuilder(doc, path)
        builder.init()
        this.value = builder.build()
    }

    fun arrayObject(init: HJsonCollectionBuilder.() -> Unit) {
        this.validate(this)
        val builder = HJsonCollectionBuilder(doc, path)
        builder.init()
        this.value = builder.build(HJsonDocument.ARRAY)
    }

    fun listObject(init: HJsonCollectionBuilder.() -> Unit) {
        this.validate(this)
        val builder = HJsonCollectionBuilder(doc, path)
        builder.init()
        this.value = builder.build(HJsonDocument.LIST)
    }

    fun setObject(init: HJsonCollectionBuilder.() -> Unit) {
        this.validate(this)
        val builder = HJsonCollectionBuilder(doc, path)
        builder.init()
        this.value = builder.build(HJsonDocument.SET)
    }

    fun mapObject(init: HJsonMapBuilder.() -> Unit) {
        this.validate(this)
        val builder = HJsonMapBuilder(doc, path)
        builder.init()
        this.value = builder.build()
    }

    fun objectJson(init: HJsonObjectBuilder.() -> Unit) {
        this.validate(this)
        val builder = HJsonObjectBuilder(doc, path)
        builder.init()
        this.value = builder.build()
    }

    fun objectReferenceable(className: String, init: HJsonObjectBuilder.() -> Unit) {
        this.validate(this)
        val builder = HJsonObjectBuilder(doc, path)
        builder.init()
        this.value = builder.build(path, className)
    }

}