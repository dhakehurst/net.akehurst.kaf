package net.akehurst.kaf.common.realisation

import net.akehurst.kaf.common.api.AFApplication
import net.akehurst.kaf.common.api.Application
import net.akehurst.kaf.service.api.Service
import kotlin.reflect.KClass

actual inline fun afApplication(self: Application, id: String, init: AFApplicationDefault.Builder.() -> Unit): AFApplication {
    val builder = AFApplicationDefault.Builder(self, id)
    builder.init()
    return builder.build()
}

actual class AFApplicationDefault(
        override val self: Application,
        afIdentity: String,
        val services: Map<String, Service>,
        initialise: () -> Unit,
        execute: () -> Unit,
        terminate: () -> Unit
) : AFPassiveDefault(self, afIdentity), AFApplication {


    actual class Builder(
            val self: Application,
            val id: String
    ) {
        actual var initialise: suspend () -> Unit
            get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
            set(value) {}
        actual var execute: suspend () -> Unit
            get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
            set(value) {}
        actual var terminate: suspend () -> Unit
            get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
            set(value) {}

        actual inline fun <reified T : Service> defineService(serviceClass: KClass<T>, noinline func: (commandLineArgs: List<String>) -> T) {
        }

        actual fun build(): AFApplication {
            TODO("not implemented")
        }


    }

    override fun startAsync(commandLineArgs: List<String>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun startBlocking(commandLineArgs: List<String>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun shutdown() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun terminate() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}