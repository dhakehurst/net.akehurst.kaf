package net.akehurst.kaf.simple.hellouser.engineering.greeter2gui


import kotlin.test.Test
import com.soywiz.korio.async.suspendTest
import kotlinx.coroutines.delay

class test_Greeter2Gui {


    //@Test
    fun t() = suspendTest {

        val sut = Greeter2Gui()

        sut.start()

        delay(5000)

        sut.started()


        delay(50000)
    }

}