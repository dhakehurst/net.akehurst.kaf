package net.akehurst.kaf.simple.hellouser.technology.cl.api

import kotlinx.io.charsets.Charset
import kotlinx.io.charsets.Charsets
import kotlinx.io.charsets.encodeToByteArray
import kotlinx.io.core.Output

interface OutputStream {

    fun write(content:String)

}

interface Environment {
    val variable:Map<String,String>
}

interface Console {

    val stdout : OutputStream

    val environment: Environment

}