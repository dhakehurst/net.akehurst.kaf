package net.akehurst.kaf.common

import net.akehurst.kaf.api.AFComponent
import net.akehurst.kaf.api.Port

open class AFComponentDefault(
        afIdentity: String,
        initialise: () -> Unit,
        execute: () -> Unit,
        terminate: () -> Unit
) : AFActiveDefault(afIdentity, initialise, execute, terminate), AFComponent {

    override val ports: Set<Port>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

}

