package net.akehurst.kaf.technology.persistence.neo4j

data class A(val prop:String)

data class Contact(
        val alias: String
) {
    constructor(alias: String, name: String, email: String, phone: String) : this(alias) {
        this.name = name
        this.email = email
        this.phone = phone
    }

    var name: String? = null
    var email: String? = null
    var phone: String? = null

}