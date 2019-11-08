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

package net.akehurst.kaf.technology.persistence.neo4j

import net.akehurst.kaf.common.api.Owner
import net.akehurst.kaf.common.api.Passive
import net.akehurst.kaf.common.realisation.afPassive
import org.neo4j.driver.v1.Driver
import org.neo4j.driver.v1.Record
import org.neo4j.driver.v1.Value

class Neo4JReader(
        override val owner: Owner,
        afId: String,
        val neo4j: Driver
) : Passive {

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
    override val af = afPassive(this,afId) {}

}