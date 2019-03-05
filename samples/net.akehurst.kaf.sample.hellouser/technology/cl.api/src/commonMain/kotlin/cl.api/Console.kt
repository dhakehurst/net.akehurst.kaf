package net.akehurst.kaf.simple.hellouser.technology.cl.api

interface OutputStream {

    fun write(content: String)

}

interface Environment {
    val variable: Map<String, String>
}

interface Console {

    val stdout: OutputStream

    val environment: Environment

}