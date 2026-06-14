package net.akehurst.kaf.technology.persistence.jdbc

import net.akehurst.kaf.technology.persistence.api.PersistenceException
import kotlin.reflect.KClass

object JdbcKotlinTypeMapping {

    fun toJdbc(kotlinType:KClass<*>) :JdbcType {
        return when(kotlinType) {
            Int::class -> JdbcType.INTEGER
            String::class -> JdbcType.VARCHAR
            else -> throw JdbcPersistenceException("Mapping unsupported for $kotlinType")
        }
    }

    fun toKotlin(jdbcType:JdbcType) : KClass<*> {
        return when(jdbcType) {
            JdbcType.INTEGER -> Int::class
            JdbcType.VARCHAR -> String::class
            else -> throw JdbcPersistenceException("Mapping unsupported for jdbc type $jdbcType")
        }
    }

}