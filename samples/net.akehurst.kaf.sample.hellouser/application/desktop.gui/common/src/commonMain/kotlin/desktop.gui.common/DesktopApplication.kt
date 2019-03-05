package net.akehurst.kaf.sample.hellouser.application.desktop.gui.common


import net.akehurst.kaf.sample.hellouser.computational.greeter.simple.GreeterSimple
import net.akehurst.kaf.simple.hellouser.engineering.greeter2gui.Greeter2Gui


class DesktopApplication {

    //--- computational ---
    val greeter = GreeterSimple()

    //--- engineering ---
    val user2gui = Greeter2Gui()

    //--- technology ---


    fun start() {
        greeter.out = user2gui
        user2gui.greeterRequest = greeter

        user2gui.start()
        greeter.start()
    }

}