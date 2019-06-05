package net.akehurst.kaf.technology.persistence.jdbc

import net.akehurst.kaf.technology.persistence.api.PersistenceException
import kotlin.reflect.KClass


actual class JdbcConnection actual constructor(val settings:Map<String,String>) {

    actual fun execute(sqlStatements:List<String>) {

    }
}