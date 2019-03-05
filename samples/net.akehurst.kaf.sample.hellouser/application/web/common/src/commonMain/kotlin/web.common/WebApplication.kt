package web.common

class WebApplication {

    //--- computational ---
    val greeter = GreeterSimple()

    //--- engineering ---
    val user2cl = Greeter2Gui()

    //--- technology ---
    val gui =


    fun start() {
        greeter.out = user2cl
        user2cl.console = console

        greeter.start()
    }


}