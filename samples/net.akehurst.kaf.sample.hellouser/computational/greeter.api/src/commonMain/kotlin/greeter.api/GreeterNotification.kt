package net.akehurst.kaf.sample.hellouser.greeter.api

interface GreeterNotification
{
    fun started()

    fun sendMessage(message:Message)
}