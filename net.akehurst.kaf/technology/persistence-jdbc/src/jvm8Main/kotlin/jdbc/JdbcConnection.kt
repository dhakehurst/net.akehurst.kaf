package net.akehurst.kaf.technology.persistence.jdbc

import net.akehurst.kaf.technology.persistence.api.PersistenceException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Types
import kotlin.reflect.KClass


actual class JdbcConnection actual constructor(val settings:Map<String,Any>) {

    val db by lazy {
        val cs = settings[PersistentStoreJdbc.URL] as String? ?: throw PersistenceException("a jdbc 'url' must be provided in the configuration settings")
        val driver = settings[PersistentStoreJdbc.DRIVER] as String? ?: throw PersistenceException("a 'driver' name must be provided in the configuration settings")
        val user = settings[PersistentStoreJdbc.USER] as String? ?: ""
        val password = settings[PersistentStoreJdbc.PASSWORD] as String? ?: ""
        Database.connect(url = cs, driver = driver, user = user, password = password)
    }

    actual fun execute(sqlStatements:List<String>) {
        val stm = this.db.connector().createStatement()
        sqlStatements.forEach {
            stm.addBatch(it)
        }
        stm.executeBatch()
    }
}