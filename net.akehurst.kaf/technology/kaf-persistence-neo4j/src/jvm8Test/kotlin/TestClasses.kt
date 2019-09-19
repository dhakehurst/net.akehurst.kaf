package net.akehurst.kaf.technology.persistence.neo4j

import com.soywiz.klock.DateTime
import com.soywiz.klock.TimeSpan

data class A(val prop:String)

data class AddressBook(
     val title: String
) {
    var contacts = mutableMapOf<String, Contact>()
}

data class Contact(
        val alias: String
) {
    var name: String? = null
    var emails: List<String> = mutableListOf()
    var phone: Set<PhoneNumber> = mutableSetOf()
    var dateOfBirth: DateTime = DateTime.EPOCH

    val age : TimeSpan get() = DateTime.now() - this.dateOfBirth

    var friendsWith = mutableSetOf<Contact>()
}

data class PhoneNumber(
        val label:String,
        val number:String
)