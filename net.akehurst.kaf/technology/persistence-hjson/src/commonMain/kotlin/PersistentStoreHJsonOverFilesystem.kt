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
package net.akehurst.kaf.technology.persistence.hjson

import com.soywiz.klock.DateTime
import net.akehurst.hjson.*
import net.akehurst.kaf.common.api.Component
import net.akehurst.kaf.common.api.Identifiable
import net.akehurst.kaf.common.api.Owner
import net.akehurst.kaf.common.api.externalConnection
import net.akehurst.kaf.common.realisation.afComponent
import net.akehurst.kaf.service.configuration.api.configuredValue
import net.akehurst.kaf.technology.persistence.api.PersistentStore
import net.akehurst.kaf.technology.persistence.fs.api.PersistenceFilesystem
import net.akehurst.kotlin.komposite.api.PrimitiveMapper
import net.akehurst.kotlin.kserialisation.hjson.KSerialiserHJson
import kotlin.reflect.KClass

class PersistentStoreHJsonOverFilesystem(
) : PersistentStore, Component {

    class FoundReferenceException : RuntimeException {
        constructor() : super()
    }

    val uriPrefix: String by configuredValue { "./" }

    var fs: PersistenceFilesystem by externalConnection<PersistenceFilesystem>()

    private val serialiser = KSerialiserHJson()

    @ExperimentalStdlibApi
    private fun buildHJson(rootItem: Any): ByteArray {
        val doc = this.serialiser.toHJson(rootItem, rootItem)
        val bytes = doc.toHJsonString().encodeToByteArray()
        return bytes
    }

    private fun calcUri(objectIdentity: String): String {
        return this.uriPrefix + objectIdentity + ".hjson"
    }

    // --- KAF ---
    override val af = afComponent {
        port("persist") {
            provides(PersistentStore::class)
        }
        initialise = { self ->
            self.af.port["persist"].connectInternal(self)
        }
    }

    // --- PersistentStore ---
    override fun configure(settings: Map<String, Any>) {
        val defaultPrimitiveMappers = mutableMapOf<KClass<*>, PrimitiveMapper<*, *>>()
        defaultPrimitiveMappers[DateTime::class] = PrimitiveMapper.create(DateTime::class, HJsonString::class,
                { primitive ->
                    val str = primitive.toString("yyyy-MM-dd'T'HH:mm:ssXXX")
                    HJsonString(str)
                },
                { raw ->
                    DateTime.parse(raw.value).local
                })
        val komposite = settings["komposite"] as List<String>
        if (settings.containsKey("primitiveMappers")) {
            defaultPrimitiveMappers.putAll(settings["primitiveMappers"] as Map<KClass<Any>, PrimitiveMapper<Any, HJsonValue>>)
        }
        af.log.debug { "trying: to register komposite information: $komposite" }
        this.serialiser.registerKotlinStdPrimitives()
        komposite.forEach {
            this.serialiser.confgureDatatypeModel(it)
        }
        (defaultPrimitiveMappers as Map<KClass<Any>, PrimitiveMapper<Any, HJsonValue>>).forEach { (k, v) ->
            this.serialiser.registerPrimitiveAsObject(k as KClass<Any>, v.toRaw, v.toPrimitive)
        }
    }

    @ExperimentalStdlibApi
    override fun <T : Any> create(type: KClass<T>, item: T, identity:T.()->String) {
        val uri = calcUri(item.identity())
        val bytes = buildHJson(item)
        this.fs.write(uri, bytes)
    }

    @ExperimentalStdlibApi
    override fun <T : Any> createAll(type: KClass<T>, itemSet: Set<T>, identity:T.()->String) {
        itemSet.forEach {
            this.create(type, it, identity)
        }
    }

    @ExperimentalStdlibApi
    override fun <T : Any> read(type: KClass<T>, identity: String): T {
        val uri = calcUri(identity)
        val bytes = fs.read(uri)
        val hjsonStr = bytes.decodeToString()
        val item = this.serialiser.toData<T>(hjsonStr)
        return item
    }

    override fun <T : Any> readAllIdentity(type: KClass<T>): Set<String> {
        TODO("not implemented")
    }

    @ExperimentalStdlibApi
    override fun <T : Any> readAll(type: KClass<T>, identities: Set<String>): Set<T> {
        val result = identities.map {
            this.read(type, it)
        }.toSet()
        return result
    }

    override fun <T : Any> update(type: KClass<T>, item: T, oldIdentity:String, newIdentity:T.()->String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T : Any> updateAll(type: KClass<T>, itemSet: Set<T>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T : Any> delete(type: KClass<T>,identity: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T : Any> deleteAll(type: KClass<T>,identitySet: Set<String>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}