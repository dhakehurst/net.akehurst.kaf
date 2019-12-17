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
import net.akehurst.kaf.technology.persistence.api.PersistenceException
import net.akehurst.kaf.technology.persistence.api.PersistentStore
import net.akehurst.kaf.technology.persistence.fs.api.PersistenceFilesystem
import net.akehurst.kotlin.komposite.api.PrimitiveMapper
import net.akehurst.kotlin.kserialisation.hjson.KSerialiserHJson
import kotlin.reflect.KClass

class PersistentStoreHJsonOverMapOfString(
) : PersistentStore, Component {

    data class Index(
            val kClass: KClass<*>,
            val identity:String
    )

    var map = mutableMapOf<Index,String>()

    private val serialiser = KSerialiserHJson()

    private fun buildHJson(rootItem: Any): String {
        val doc = this.serialiser.toHJson(rootItem, rootItem)
        val str = doc.toHJsonString()
        return str
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

    override fun <T : Any> create(type: KClass<T>, item: T, identity:T.()->String) {
        val id = Index(type, item.identity())
        val hjsonStr = buildHJson(item)
        this.map[id] = hjsonStr
    }

    override fun <T : Any> createAll(type: KClass<T>, itemSet: Set<T>, identity:T.()->String){
        itemSet.forEach {
            this.create(type, it, identity)
        }
    }

    override fun <T : Any> read(type: KClass<T>, identity: Any): T {
        val id = Index(type, identity.toString())
        val hjsonStr = this.map[id] ?: throw PersistenceException("Item of type ${type.simpleName} not found with identity $identity")
        val item = this.serialiser.toData<T>(hjsonStr)
        return item
    }

    override fun <T : Any> readAllIdentity(type: KClass<T>): Set<String> {
        return this.map.keys.filter { it.kClass==type }.map { it.identity }.toSet()
    }

    override fun <T : Any> readAll(type: KClass<T>, identities: Set<Any>): Set<T> {
        val result = identities.map {
            this.read(type, it)
        }.toSet()
        return result
    }

    override fun <T : Any> update(type: KClass<T>, item: T) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T : Any> updateAll(type: KClass<T>, itemSet: Set<T>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T : Any> delete(identity: Any) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T : Any> deleteAll(identitySet: Set<Any>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}