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

import korlibs.time.DateTime
import korlibs.time.TimeSpan
import net.akehurst.kaf.common.api.Identifiable

data class A(val prop: String) : Identifiable {
    override val identity: String = prop
}

data class AddressBook(
        val title: String
) : Identifiable {
    var contacts = mutableMapOf<String, Contact>()

    override val identity: String = title
}

data class Contact(
        val alias: String
) : Identifiable {
    var name: String? = null
    var emails: MutableList<String> = mutableListOf()
    var phone: MutableSet<LabelledPhoneNumber> = mutableSetOf()
    var dateOfBirth: DateTime = DateTime.EPOCH

    val age: TimeSpan get() = DateTime.now() - this.dateOfBirth

    var friendsWith: MutableSet<Contact> = mutableSetOf<Contact>()

    override val identity: String = alias
}

data class LabelledPhoneNumber(
        val label: String,
        val number: PhoneNumber
)

data class ContainsAnAnyProp(
    val number:Int,
    val something:Any?
) : Identifiable {
    override val identity: String get() = "id$number"
}

inline class PhoneNumber(
    val value:String
)