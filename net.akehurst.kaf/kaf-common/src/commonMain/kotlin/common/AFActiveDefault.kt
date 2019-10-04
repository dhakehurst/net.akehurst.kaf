package net.akehurst.kaf.common

import net.akehurst.kaf.api.AFActive
import net.akehurst.kaf.api.Active


inline fun afActive(self: Active, id: String, init: AFActiveDefault.Builder.() -> Unit = {}): AFActive {
    val builder = AFActiveDefault.Builder(self, id)
    builder.init()
    return builder.build()
}

open class AFActiveDefault(
        afIdentity: String,
        val initialise: () -> Unit,
        val execute: () -> Unit,
        val terminate: () -> Unit
) : AFIdentifiableDefault(afIdentity), AFActive {

    class Builder(val self: Active, val id: String) {
        var initialise: () -> Unit = {}
        var execute: () -> Unit = {}
        var terminate: () -> Unit = {}
        fun build(): AFActive {
            return AFActiveDefault(id, initialise, execute, terminate)
        }
    }


    override fun start() {
        log.trace { "start" }
        this.initialise()
        this.execute()
    }

    override fun join() {
        log.trace { "join" }
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun stop() {
        log.trace { "stop" }
        this.terminate()
    }
}
