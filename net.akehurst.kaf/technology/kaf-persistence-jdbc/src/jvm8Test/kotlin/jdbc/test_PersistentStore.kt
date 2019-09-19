package net.akehurst.kaf.technology.persistence.jdbc

import kotlin.test.Test

class test_PersistentStore {

    @Test
    fun create() {

        val sut = PersistentStoreJdbc()
        sut.configure(mapOf(
                PersistentStoreJdbc.URL to "jdbc:postgresql://localhost:5432/testKaf?user=vistraq", //"jdbc:h2:mem:regular",
                PersistentStoreJdbc.DRIVER to "org.postgresql.Driver" //"org.h2.Driver"
        ))

        val p1 = Contact("dave", "David", "dave@email.adddess", "+0123456789")

        sut.create(Contact::class, p1)

    }

}