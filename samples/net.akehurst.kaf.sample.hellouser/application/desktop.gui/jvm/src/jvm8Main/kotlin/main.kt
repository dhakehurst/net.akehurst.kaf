package net.akehurst.kaf.sample.hellouser.application.desktop.jvm

import net.akehurst.kaf.sample.hellouser.application.desktop.gui.common.DesktopApplication


object Main {
    @JvmStatic
    fun main(args : Array<String>) {
        val application = DesktopApplication()
        application.start()
    }
}