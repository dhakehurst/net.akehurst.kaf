package net.akehurst.kaf.technology.persistence.neo4j

import net.akehurst.kaf.technology.persistence.api.PersistentStore
import org.neo4j.driver.v1.AuthTokens
import org.neo4j.driver.v1.Driver
import org.neo4j.driver.v1.GraphDatabase
import kotlin.reflect.KClass
import net.akehurst.kaf.api.Component
import net.akehurst.kaf.common.afComponent
import net.akehurst.kaf.service.configuration.api.configuredValue
import net.akehurst.kaf.technology.persistence.api.PersistenceException
import net.akehurst.kotlin.komposite.common.DatatypeRegistry
import net.akehurst.kotlin.komposite.common.KompositeWalker
import net.akehurst.kotlin.komposite.common.WalkInfo
import net.akehurst.kotlin.komposite.common.kompositeWalker
import net.akehurst.kotlinx.collections.Stack
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.kernel.configuration.BoltConnector
import java.io.File

interface CypherStatement {
    fun toCypher() : String
}

data class CypherProperty(val name:String, val value:Any?) {
    fun toCypher(): String = "${name}: ${value}"
}

data class CypherNodeMerge(
        val label:String
) : CypherStatement {
    val properties = mutableListOf<CypherProperty>()
    override fun toCypher() : String {
        val propertyStr = this.properties.map { it.toCypher() }.joinToString(", ")
        return "MERGE (:`$label`{$propertyStr})"
    }
}



class PersistentStoreNeo4j(
        afId: String
) : PersistentStore, Component {

    private val _registry = DatatypeRegistry()

    // --- injected ---
    val embeddedNeo4jDirectory: String by configuredValue("configuration") { ".neo4j" }
    val embeddedNeo4jAddress: String by configuredValue("configuration") { "localhost" }
    val embeddedNeo4jPort: Int by configuredValue("configuration") { 7777 }

    // --- set when config is called ---
    private var _neo4jService: GraphDatabaseService? = null
    private lateinit var _neo4j: Driver

    private fun executeReadCypher(cypher:String) {
        af.log.trace { "executeReadCypher($cypher)" }
        this._neo4j.session().use { session ->
            session.readTransaction { tx ->
                val result = tx.run(cypher)
            }
        }
    }
    private fun executeWriteCypher(cypher:String) {
        af.log.trace { "executeWriteCypher($cypher)" }
        this._neo4j.session().use { session ->
            session.writeTransaction { tx ->
                val result = tx.run(cypher)
            }
        }
    }
    // --- KAF ---
    override val af = afComponent(this, afId) {
        terminate = {
            if (null!=_neo4jService) {
                _neo4jService?.shutdown()
            }
        }
    }


    // --- PersistentStore ---
    override fun configure(settings: Map<String, Any>) {
        val embedded = settings["embedded"] as Boolean? ?: false
        if (embedded) {
            af.log.info { "starting embedded neo4j database in directory $embeddedNeo4jDirectory at $embeddedNeo4jAddress:$embeddedNeo4jPort" }
            val embeddedDirectory = File("${this.embeddedNeo4jDirectory}/data")
            val bolt = BoltConnector("0")
            this._neo4jService = GraphDatabaseFactory() //
                    .newEmbeddedDatabaseBuilder(embeddedDirectory)
                    .setConfig(bolt.type, "BOLT")
                    .setConfig(bolt.enabled, "true")
                    .setConfig(bolt.listen_address, "$embeddedNeo4jAddress:$embeddedNeo4jPort")
                    .newGraphDatabase()

            // Registers a shutdown hook for the Neo4j instance so that it
            // shuts down nicely when the VM exits (even if you "Ctrl-C" the
            // running application).
            Runtime.getRuntime().addShutdownHook(object : Thread() {
                override fun run() {
                    _neo4jService?.shutdown()
                }
            })
        }

        val uri = settings["uri"] as String
        val user = settings["user"] as String
        val password = settings["password"] as String
        af.log.debug { "trying: to connect to Neo4j: ${uri} as user ${user}" }
        this._neo4j = GraphDatabase.driver(uri, AuthTokens.basic(user, password))
        this._neo4j.session().use { session ->
            session.readTransaction { tx ->
                tx.run("RETURN 'Hello Neo4j'")
            }
        }
        af.log.debug { "success: connected to Neo4j: ${uri} as user ${user}" }

        val komposite = settings["komposite"] as String
        af.log.debug { "trying: to register komposite information: $komposite" }
        this._registry.registerFromConfigString(DatatypeRegistry.KOTLIN_STD)
        this._registry.registerFromConfigString(komposite)
    }

    override fun <T : Any> create(identity: String, type: KClass<T>, item: T) {
        af.log.trace { "create($identity, ${type.simpleName}, ...)" }
        var currentObjStack = Stack<CypherStatement>()
        val walker = kompositeWalker<List<String>, Any?>(this._registry) {
            nullValue { key, info ->
                val path = if (key == KompositeWalker.ROOT) info.path else info.path + key.toString()
                af.log.debug { "walk: nullValue: $path = null" }
                WalkInfo(path, null)
            }
            primitive { key, info, value ->
                af.log.debug { "walk: primitive: $key, $info" }
                val path = if (key == KompositeWalker.ROOT) info.path else info.path + key.toString()
                val cypherValue = when(value) {
                    is String -> "'${value}'"
                    else -> "${value}"
                }
                WalkInfo(path, cypherValue)
            }
            objectBegin { key, info, obj, datatype ->
                af.log.debug { "walk: objectBegin: $key, $info" }
                val path = if (key == KompositeWalker.ROOT) info.path else info.path + key.toString()
                val obj = CypherNodeMerge(datatype.qualifiedName("."))
                currentObjStack.push(obj)
                WalkInfo(path, obj)
            }
            objectEnd { key, info, obj, datatype ->
                af.log.debug { "walk: objectEnd: $key, $info" }
                val obj = currentObjStack.pop()
                WalkInfo(info.path, obj)
            }
            propertyBegin { key, info, property ->
                af.log.debug { "walk: propertyBegin: $key, $info" }
                info
            }
            propertyEnd { key, info, property ->
                af.log.debug { "walk: propertyEnd: $key, $info" }
                val path = if (key == KompositeWalker.ROOT) info.path else info.path + key.toString()
                val cuObj = currentObjStack.peek() as CypherNodeMerge
                cuObj.properties.add(CypherProperty(key as String, info.acc))
                //currentObjStack.push(nObj)
                WalkInfo(path, cuObj)
            }
        }
        val result = walker.walk(WalkInfo(emptyList(), null), item)

        val cypher = (result.acc as CypherStatement).toCypher()
        this.executeWriteCypher(cypher)
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