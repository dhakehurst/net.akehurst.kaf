package net.akehurst.kaf.api

import net.akehurst.kaf.service.logging.api.Logger

interface AFIdentifiable {
    val identity: String
    val log: Logger
    fun doInjections(root:Identifiable)
}
interface Identifiable {
    val af:AFIdentifiable
}

interface AFActive : AFIdentifiable {
    fun start()
    fun join()
    fun stop()
}
interface Active : Identifiable {
    override val af:AFActive
}

interface Port {

}
interface AFComponent : AFActive {
    val ports : Set<Port>
}
interface Component : Active {
    override val af: AFComponent
}

interface AFApplication : AFComponent {
    fun start(commandLineArgs: List<String>)
}
interface Application : Component {
    override val af: AFApplication
}