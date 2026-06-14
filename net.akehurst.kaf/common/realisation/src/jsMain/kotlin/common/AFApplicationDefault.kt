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
    actual override val self: Application,
    actual override val identity: String,
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

    actual override var afHolder: AFHolder? = self
    actual override var selfIdentity: String? = identity

    private val _services = mutableMapOf<KClass<*>, Service>()
    actual override val framework by serviceReference<ApplicationFrameworkService>()

    actual override fun <T : Service> service(serviceClass: KClass<T>): T {
        TODO()
    }

    actual override fun startAsync(commandLineArgs: List<String>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    actual override fun startBlocking(commandLineArgs: List<String>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    actual override fun shutdown() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    actual override fun terminate() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}