package net.akehurst.kaf.technology.persistence.api

import kotlin.reflect.KClass


open class PersistenceException : RuntimeException {
    constructor(message:String) : super(message)
}

interface PersistentStore {

    fun configure(settings:Map<String, Any>)

    fun <T : Any> create(type: KClass<T>, item: T)
    fun <T : Any> createAll(type: KClass<T>, itemSet: Set<T>)
    fun <T : Any> read(type: KClass<T>, filterSet: Set<Filter> = emptySet()): T
    fun <T : Any> readAll(type: KClass<T>, filterSet: Set<Filter>): Set<T>
    fun <T : Any> update(type: KClass<T>, item: T, filterSet: Set<Filter> = emptySet())
    fun <T : Any> updateAll(type: KClass<T>, itemSet: Set<T>)
    fun <T : Any> delete(identity: String)
    fun <T : Any> deleteAll(identitySet: Set<String>)

}

interface Filter {
}

data class FilterProperty(
        val propertyName:String,
        val value:Any
) : Filter
