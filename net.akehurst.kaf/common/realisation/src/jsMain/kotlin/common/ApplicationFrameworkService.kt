package net.akehurst.kaf.common.realisation

import net.akehurst.kaf.common.api.*
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty


actual fun <T> runBlocking(context: CoroutineContext,block:suspend () -> T): T {
    TODO()
}

actual class ApplicationFrameworkServiceDefault() : ApplicationFrameworkService {

    actual override fun makeAccessible(callable: KCallable<*>) {
        //all are accessible on JS
    }

    actual override fun callOn(obj: Any, callableName: String): Any {
        TODO("not implemented")
    }
    actual override fun doInjections(commandLineArgs: List<String>, root: AFHolder) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    actual override fun partsOf(composite: Owner): List<Passive> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    actual override fun externalConnections(self: Passive, kclass: KClass<*>): Map<KProperty<*>, ExternalConnection<*>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
    actual override fun shutdown() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    actual override fun terminate() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}