package net.akehurst.kaf.sample.hellouser.greeter.api

interface GreeterRequest
{

    fun start()

    fun authenticate(credentials: Credentials)
}