package net.akehurst.kaf.api

import net.akehurst.kaf.service.logging.api.Logger

interface AFIdentifiable {
    val identity: String
    val logger: Logger
}
interface Identifiable {
    val af:AFIdentifiable
}

interface AFActive : AFIdentifiable {
    fun start()
    fun join()
    fun stop()
}
interface Active {
    val af:AFActive
}

interface AFComponent : AFActive
interface Component

interface AFApplication : AFComponent {
    fun start(commandLineArgs: List<String>)
}
interface Application {
    val af: AFApplication
}