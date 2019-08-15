package net.akehurst.kaf.technology.persistence.neo4j

import net.akehurst.kaf.technology.persistence.api.PersistentStore
import org.neo4j.driver.v1.AuthTokens
import org.neo4j.driver.v1.Driver
import org.neo4j.driver.v1.GraphDatabase
import kotlin.reflect.KClass


class PersistentStoreNeo4j : PersistentStore, Component {

    private lateinit var _neo4j: Driver

    override fun configure(settings: Map<String, String>) {
        val uri = settings["uri"]
        val user = settings["user"]
        val password = settings["password"]
        this._neo4j = GraphDatabase.driver(uri, AuthTokens.basic(user, password))
        this._neo4j.session().use { session ->
            session.readTransaction { tx ->
                tx.run("RETURN 'Hello Neo4j'")
            }
        }
        log.debug { "connected to Neo4j: ${uri} as user ${user}" }

    }

    override fun <T : Any> create(identity: String, type: KClass<T>, item: T) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T : Any> createAll(identity: String, type: KClass<T>, items: Set<T>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T : Any> read(identity: String, type: KClass<T>): T {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T : Any> readAll(identity: String, type: KClass<T>): Set<T> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T : Any> update(identity: String, type: KClass<T>, item: T) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T : Any> updateAll(identity: String, type: KClass<T>, items: Set<T>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T : Any> delete(identity: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T : Any> deleteAll(identitySet: Set<String>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}