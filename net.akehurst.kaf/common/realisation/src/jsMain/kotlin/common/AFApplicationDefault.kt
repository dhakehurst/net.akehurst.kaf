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
        override val identity: String,
        val defineServices: Map<KClass<*>, (commandLineArgs: List<String>) -> Service>,
        initialise: () -> Unit,
        execute: () -> Unit,
        finalise: () -> Unit
) : AFApplication {
    actual class Builder(
            val self: Application,
            val id: String
    ) {
        actual var initialise: suspend (self:Application) -> Unit
            get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
            set(value) {}
        actual var execute: suspend (self:Application) -> Unit
            get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
            set(value) {}
        actual var finalise: suspend (self:Application) -> Unit
            get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
            set(value) {}

        actual inline fun <reified T : Service> defineService(serviceClass: KClass<T>, noinline func: (commandLineArgs: List<String>) -> T) {
        }

        actual fun build(): AFApplication {
            TODO("not implemented")
        }
    }

    private val _services = mutableMapOf<KClass<*>, Service>()
    override fun <T : Service> service(serviceClass: KClass<T>): T {
        TODO()
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