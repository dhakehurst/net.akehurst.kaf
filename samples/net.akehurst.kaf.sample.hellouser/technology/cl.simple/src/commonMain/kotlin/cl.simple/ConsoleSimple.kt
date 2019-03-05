package net.akehurst.kaf.simple.hellouser.technology.cl.simple

import net.akehurst.kaf.simple.hellouser.technology.cl.api.Console
import net.akehurst.kaf.simple.hellouser.technology.cl.api.OutputStream

class ConsoleSimple : Console {

    override val stdout = object : OutputStream {
        override fun write(content: String) {
            print(content)
        }
    }

}