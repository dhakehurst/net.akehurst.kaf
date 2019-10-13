package net.akehurst.kaf.common.realisation

import net.akehurst.kaf.common.api.ApplicationFrameworkService
import net.akehurst.kaf.common.api.Identifiable
import kotlin.js.Promise
import kotlin.reflect.KCallable
import kotlin.reflect.KClass


actual fun <T> runBlocking(block:suspend () -> T): T {
    TODO()
}

actual class ApplicationFrameworkServiceDefault() : ApplicationFrameworkService {
    actual override fun <T : Any> receiver(forInterface: KClass<*>, invokeMethod: (proxy: Any?, callable: KCallable<*>, args: Array<out Any>) -> Any?): T {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
    actual override fun doInjections(commandLineArgs: List<String>, root: Identifiable) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    actual override fun partsOf(composite: Identifiable): List<Identifiable> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    actual override fun shutdown() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    actual override fun terminate() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}