package net.akehurst.kaf.common

import net.akehurst.kaf.api.AFApplication
import net.akehurst.kaf.api.Application
import net.akehurst.kaf.api.Identifiable
import net.akehurst.kaf.service.api.Service

actual class ApplicationFrameworkService(){
    actual fun doInjections(commandLineArgs: List<String>, root: Identifiable) {
    }

}

actual inline fun afApplication(self: Application, id: String, init: AFApplicationDefault.Builder.() -> Unit): AFApplication {
    val builder = AFApplicationDefault.Builder(self, id)
    builder.init()
    return builder.build()
}

actual class AFApplicationDefault(
        val self: Application,
        afIdentity: String,
        val services: Map<String, Service>,
        initialise: () -> Unit,
        execute: () -> Unit,
        terminate: () -> Unit
) : AFComponentDefault(afIdentity, initialise, execute, terminate), AFApplication {
    actual class Builder(
            val self: Application,
            val id: String
    ) {
        actual var initialise: () -> Unit
            get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
            set(value) {}
        actual var execute: () -> Unit
            get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
            set(value) {}
        actual var terminate: () -> Unit
            get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
            set(value) {}
        actual var defineServices: (commandLineArgs: List<String>) -> Map<String, Service>
            get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
            set(value) {}

        actual fun build() : AFApplication {
            TODO("not implemented")
        }


    }

    override fun start(commandLineArgs: List<String>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}