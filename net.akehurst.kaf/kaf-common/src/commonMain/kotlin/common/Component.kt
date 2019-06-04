package net.akehurst.kaf.common

import net.akehurst.kaf.api.AFComponent

open class AFComponentDefault(
        afIdentity: String,
        initialise: () -> Unit,
        execute: () -> Unit,
        terminate: () -> Unit
) : AFActiveDefault(afIdentity, initialise, execute, terminate), AFComponent {

}

