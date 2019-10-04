package net.akehurst.kaf.technology.persistence.neo4j

import net.akehurst.kaf.api.Identifiable
import net.akehurst.kaf.common.afIdentifiable
import org.neo4j.driver.v1.Driver
import org.neo4j.driver.v1.Record
import org.neo4j.driver.v1.Value

class Neo4JReader(
        afId: String,
        val neo4j: Driver
) : Identifiable {

    fun executeReadCypher(cypherStatements: List<CypherStatement>): List<Record> {
        //TODO: use 'USING PERIODIC COMMIT' to improve performance
        val records = mutableListOf<Record>()
        this.neo4j.session().use { session ->
            session.readTransaction { tx ->
                cypherStatements.forEach { stm ->
                    val cypherStr = stm.toCypherStatement()
                    af.log.trace { "executeReadCypher($cypherStr)" }
                    val result = tx.run(cypherStr)
                    records.addAll(result.list())
                }
            }
        }
        return records
    }

    fun recordsToPathMap(records: List<Record>): MutableMap<String, Value> {
        val map = mutableMapOf<String, Value>()
        records.forEach { rec ->
            val path = rec.keys().first()
            map[path] = rec[path]
            rec.keys().forEach { key ->
                when {
                    key.endsWith(CypherStatement.ENTRY_PATH_SEGMENT) -> {
                        val valueNode = rec[key].asNode()
                        val entryPath = valueNode[CypherStatement.PATH_PROPERTY].asString()
                        map[entryPath] = rec[key]
                    }
                }
            }
        }
        return map //records.groupBy({ it.keys().first() }, { val key = it.keys().first(); it[key] })
    }

    // --- KAF ---
    override val af = afIdentifiable(this,afId) {}

}