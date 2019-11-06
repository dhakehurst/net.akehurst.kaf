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

package net.akehurst.kaf.simple.hellouser.engineering.greeter2cl

import net.akehurst.kaf.sample.hellouser.greeter.api.Message
import net.akehurst.kaf.simple.hellouser.technology.cl.api.Console
import net.akehurst.kaf.simple.hellouser.technology.cl.api.Environment
import net.akehurst.kaf.simple.hellouser.technology.cl.api.OutputStream
import kotlin.test.Test
import kotlin.test.assertEquals


class test_Greeter2Cl {


    //@Test
    fun started() {
        val sut = Greeter2Cl("sut")
        val actual = mutableListOf<String>()
        sut.console = object : Console {
            override val stdout = object : OutputStream {
                override fun write(content: String) {
                    actual.add(content)
                }
            }
            override val environment = object : Environment {
                override val variable = mapOf<String,String>() //TODO: add user
            }
        }

        //TODO: provide greeterRequest stub

        sut.started()

        assertEquals("Started", actual[0])

    }

    @Test
    fun sendMessage() {
        val sut = Greeter2Cl("sut")
        val actual = mutableListOf<String>()
        sut.console = object : Console {
            override val stdout = object : OutputStream {
                override fun write(content: String) {
                    actual.add(content)
                }
            }
            override val environment = object : Environment {
                override val variable = mapOf<String,String>()
            }
        }

        sut.sendMessage(Message("Test"))

        assertEquals("Test", actual[0])

    }

}
