package net.akehurst.kaf.technology.persistence.jdbc

import kotlin.test.Test
import kotlin.test.assertEquals

class test_JdbcConnection {

    @Test
    fun t() {
        val settings = mapOf(
                PersistentStoreJdbc.URL to "jdbc:h2:mem:regular",
                PersistentStoreJdbc.DRIVER to "org.h2.Driver"
        )
        val sut = JdbcConnection(settings)


    }


}