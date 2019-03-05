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
        val sut = Greeter2Cl()
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
        val sut = Greeter2Cl()
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
