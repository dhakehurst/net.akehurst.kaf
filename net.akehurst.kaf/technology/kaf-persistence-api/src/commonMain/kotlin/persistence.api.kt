package net.akehurst.kaf.technology.persistence.api

import kotlin.reflect.KClass


open class PersistenceException : RuntimeException {
    constructor(message:String) : super(message)
}

interface PersistentStore {

    fun configure(settings:Map<String, Any>)

    fun <T : Any> create(identity: String, type: KClass<T>, item: T)
    fun <T : Any> createAll(identity: String, type: KClass<T>, items: Set<T>)
    fun <T : Any> read(identity: String, type: KClass<T>): T
    fun <T : Any> readAll(identity: String, type: KClass<T>): Set<T>
    fun <T : Any> update(identity: String, type: KClass<T>, item: T)
    fun <T : Any> updateAll(identity: String, type: KClass<T>, items: Set<T>)
    fun <T : Any> delete(identity: String)
    fun <T : Any> deleteAll(identitySet: Set<String>)

}
