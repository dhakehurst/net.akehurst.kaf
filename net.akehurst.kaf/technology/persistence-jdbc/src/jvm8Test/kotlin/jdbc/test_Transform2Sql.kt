package net.akehurst.kaf.technology.persistence.jdbc

import kotlin.test.Test
import kotlin.test.assertEquals


class test_Transform2Sql {

    @Test
    fun t() {
        val settings = mapOf<String,String>()
        val sut = Transformer2Sql(settings)

        val actual = sut.transform2CreateTable(Contact::class)
        val expected = listOf("CREATE TABLE IF NOT EXISTS Contact ( alias VARCHAR, email VARCHAR, name VARCHAR, phone VARCHAR )")

        assertEquals(expected, actual)
    }

}