package net.akehurst.kaf.common.realisation

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import net.akehurst.kaf.common.api.AFActor
import net.akehurst.kaf.common.api.Active
import net.akehurst.kaf.common.api.Actor
import net.akehurst.kaf.common.api.ActorException
import net.akehurst.kotlinx.reflect.reflect
import kotlin.reflect.KCallable
import kotlin.reflect.KClass


inline fun afActor(self: Actor, id: String, init: AFActorDefault.Builder.() -> Unit = {}): AFActor {
    val builder = AFActorDefault.Builder(self, id)
    builder.init()
    return builder.build()
}

class Signal(
        val name: String,
        val func: ()->Unit
) {
    fun invoke(self: Actor) {
        func.invoke()
    }
}

open class AFActorDefault(
        override val self: Actor,
        afIdentity: String,
        val initialise: () -> Unit,
        val preExecute: () -> Unit,
        val terminateFunc: () -> Unit
) : AFIdentifiableDefault(self, afIdentity), AFActor {

    class Builder(val self: Actor, val id: String) {
        var initialise: () -> Unit = {}
        var preExecute: () -> Unit = {}
        var terminate: () -> Unit = {}

        fun build(): AFActor {
            return AFActorDefault(self, id, initialise, preExecute, terminate)
        }
    }

    private val inbox = Channel<Signal>(Channel.UNLIMITED)
    private lateinit var job: Job

    override fun receive(name:String, func: () -> Unit) {
        log.trace { "received: $name" }
        inbox.offer(Signal(name, func))
    }

    override suspend fun start() {
        log.trace { "start" }
        log.trace { "initialise" }
        this@AFActorDefault.initialise()

        val activeParts = super.framework.partsOf(self).filterIsInstance<Active>()
        activeParts.forEach {
            it.af.start()
        }

        job = GlobalScope.launch {

            log.trace { "preExecute" }
            this@AFActorDefault.preExecute()

            inbox.consumeEach { signal ->
                log.trace { "invoking: ${signal.name}" }
                signal.invoke(self)
            }
            inbox.close()

        }

        activeParts.forEach {
            it.af.join()
        }

    }

    override suspend fun join() {
        log.trace { "join" }
        val activeParts = super.framework.partsOf(self).filterIsInstance<Active>()
        activeParts.forEach {
            it.af.join()
        }
        job.join()
    }

    override suspend fun shutdown() {
        log.trace { "shutdown" }
        val activeParts = super.framework.partsOf(self).filterIsInstance<Active>()
        activeParts.forEach {
            it.af.shutdown()
        }
        terminateFunc()
        this.inbox.close()
        job.join()
    }

    override suspend fun terminate() {
        log.trace { "terminate" }
        val activeParts = super.framework.partsOf(self).filterIsInstance<Active>()
        activeParts.forEach {
            it.af.terminate()
        }
        terminateFunc()
        this.inbox.cancel()
        job.cancelAndJoin()
    }

    override fun <T : Any> receiver(forInterface: KClass<*>): T {
        //TODO: cache receivers
        return super.framework.receiver(forInterface) { proxy, callable, args ->
            when {
                forInterface.isInstance(self) -> receive(callable.name) { self.reflect().call(callable.name, *args) }
                else -> throw ActorException("${self.af.identity}:${self::class.simpleName!!} does not implement ${forInterface.simpleName!!}")
            }
        }
    }

}