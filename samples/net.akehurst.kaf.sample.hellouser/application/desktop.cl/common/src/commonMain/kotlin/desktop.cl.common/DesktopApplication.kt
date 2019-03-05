package net.akehurst.kaf.sample.hellouser.application.desktop.cl.common


import net.akehurst.kaf.sample.hellouser.computational.greeter.simple.GreeterSimple
import net.akehurst.kaf.simple.hellouser.engineering.greeter2cl.Greeter2Cl
import net.akehurst.kaf.simple.hellouser.technology.cl.simple.ConsoleSimple

class DesktopApplication {

    //--- computational ---
    val greeter = GreeterSimple()

    //--- engineering ---
    val user2cl = Greeter2Cl()

    //--- technology ---
    val console = ConsoleSimple()


    fun start() {
        greeter.out = user2cl
        user2cl.console = console

        greeter.start()
    }

}