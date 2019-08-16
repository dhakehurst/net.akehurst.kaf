package net.akehurst.kaf.common

import net.akehurst.kaf.api.*
import net.akehurst.kaf.service.api.Service

inline fun afComponent(self: Component, id: String, init: AFComponentDefault.Builder.() -> Unit = {}): AFComponent {
    val builder = AFComponentDefault.Builder(self, id)
    builder.init()
    return builder.build()
}


open class AFComponentDefault(
        afIdentity: String,
        initialise: () -> Unit,
        execute: () -> Unit,
        terminate: () -> Unit
) : AFActiveDefault(afIdentity, initialise, execute, terminate), AFComponent {

    class Builder(val self: Component, val id: String) {
        var initialise: () -> Unit = {}
        var execute: () -> Unit = {}
        var terminate: () -> Unit = {}
        fun build(): AFComponent {
            return AFComponentDefault(id, initialise, execute, terminate)
        }
    }

    override val ports: Set<Port>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

}

