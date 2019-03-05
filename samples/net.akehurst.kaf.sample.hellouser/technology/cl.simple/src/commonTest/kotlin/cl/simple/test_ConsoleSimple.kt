package cl.simple

import kotlinx.io.core.toByteArray
import kotlinx.io.core.writeText
import net.akehurst.kaf.simple.hellouser.technology.cl.api.Console
import net.akehurst.kaf.simple.hellouser.technology.cl.simple.ConsoleSimple
import kotlin.test.Test
import kotlin.test.assertEquals

class test_ConsoleSimple {

    @Test
    fun stdout_write() {

        val sut:Console = ConsoleSimple()

        sut.stdout.write("Test")


    }


}