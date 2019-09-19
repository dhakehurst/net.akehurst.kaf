package net.akehurst.kaf.technology.comms.api

interface MessageChannel {

    fun receive(channelId:String, action:(endPointId:String, message: String) -> Unit)

    fun send(endPointId:String, channelId:String, message:String)

}