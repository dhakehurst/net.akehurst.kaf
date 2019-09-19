package net.akehurst.kaf.technology.persistence.jdbc

import net.akehurst.kaf.technology.persistence.api.Filter
import net.akehurst.kaf.technology.persistence.api.PersistenceException
import net.akehurst.kaf.technology.persistence.api.PersistentStore
import kotlin.reflect.KClass

class JdbcPersistenceException : PersistenceException {
    constructor(message:String) : super(message)
}

expect class JdbcConnection(settings: Map<String, Any>) {
    fun execute(sqlStatements:List<String>)
}

class PersistentStoreJdbc  : PersistentStore {

    companion object {
        val URL = "url"
        val DRIVER = "driver"
        val USER = "username"
        val PASSWORD = "password"
    }

    internal lateinit var connection : JdbcConnection
    internal lateinit var transformer : Transformer2Sql

    override fun configure(settings: Map<String, Any>) {
        this.connection = JdbcConnection(settings)
        this.transformer = Transformer2Sql(settings)
    }

    override fun <T : Any> create(type: KClass<T>, item: T) {
        val sqlStatements = this.transformer.transform2CreateItem(type, item)
        this.connection.execute(sqlStatements)
    }

    override fun <T : Any> createAll(type: KClass<T>, items: Set<T>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T : Any> read(type: KClass<T>, filterSet: Set<Filter>): T {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T : Any> readAll(type: KClass<T>, filterSet: Set<Filter>): Set<T> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T : Any> update(type: KClass<T>, item: T, filterSet: Set<Filter>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T : Any> updateAll(type: KClass<T>, items: Set<T>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T : Any> delete(identity: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T : Any> deleteAll(identitySet: Set<String>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}