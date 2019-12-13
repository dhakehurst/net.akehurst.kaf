package net.akehurst.kaf.common.realisation

import net.akehurst.kaf.common.api.*
import net.akehurst.kaf.service.api.Service
import net.akehurst.kaf.service.api.serviceReference
import kotlin.reflect.KClass

actual inline fun afApplication(self: Application, identity: String, init: AFApplicationDefault.Builder.() -> Unit): AFApplication {
    val builder = AFApplicationDefault.Builder(self, identity)
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
) :  AFDefault(identity), AFApplication {

    actual class Builder(
            val self: Application,
            val identity: String
    ) {
        actual var initialise: suspend (self: Application) -> Unit
            get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
            set(value) {}
        actual var execute: suspend (self: Application) -> Unit
            get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
            set(value) {}
        actual var finalise: suspend (self: Application) -> Unit
            get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
            set(value) {}

        actual inline fun <reified T : Service> defineService(serviceClass: KClass<T>, noinline func: (commandLineArgs: List<String>) -> T) {
        }

        actual fun build(): AFApplication {
            TODO("not implemented")
        }
    }

    override var afHolder: AFHolder? = self
    override var selfIdentity: String? = identity

    private val _services = mutableMapOf<KClass<*>, Service>()
    override val framework by serviceReference<ApplicationFrameworkService>()

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