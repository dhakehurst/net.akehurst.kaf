/**
 * Copyright (C) 2019 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.akehurst.kaf.common.realisation

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import net.akehurst.kaf.common.api.*
import net.akehurst.kotlinx.reflect.reflect
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.time.Duration
import kotlin.time.ExperimentalTime


inline fun afActor(self: Actor, id: String, init: AFActorDefault.Builder.() -> Unit = {}): AFActor {
    val builder = AFActorDefault.Builder(self, id)
    builder.init()
    return builder.build()
}

class Signal(
        val signature: KCallable<*>,
        val func: suspend () -> Unit
) {
    suspend fun invoke(self: Actor) {
        func.invoke()
    }
}

data class SignalKey(val signal: KCallable<*>, val context: AsyncCallContext) {

    //TODO: remove these and use context also as part of equality
    override fun hashCode(): Int {
        return signal.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is SignalKey -> {
                this.signal == other.signal
            }
            else -> false
        }
    }
}

open class AFActorDefault(
        override val self: Actor,
        afIdentity: String,
        val initialise: suspend () -> Unit,
        val preExecute: suspend () -> Unit,
        val finalise: suspend () -> Unit
) : AFPassiveDefault(self, afIdentity), AFActor {

    class Builder(val self: Actor, val id: String) {
        var initialise: suspend () -> Unit = {}
        var preExecute: suspend () -> Unit = {}
        var finalise: suspend () -> Unit = {}

        fun build(): AFActor {
            return AFActorDefault(self, id, initialise, preExecute, finalise)
        }
    }

    private val inbox = Channel<Signal>(Channel.UNLIMITED)
    private val whenReceived = mutableMapOf<SignalKey, MutableSet<Any>>() //key -> (...)->Unit
    private lateinit var job: Job

    private fun receive0(callable: KCallable<*>, context: AsyncCallContext, defaultBody: () -> Unit) {
        val key = SignalKey(callable, context)
        this.receive(callable, context) {
            val wrFunc = this.removeWhenReceived(key) // TODO: what if we process the receive before the whenRecieved...can this happen? and handle timeout!
            if (null != wrFunc) {
                log.trace { "using when received: ${key.signal.name}" }
                (wrFunc as () -> Unit).invoke()
            } else {
                defaultBody.invoke()
            }
        }
    }

    private fun <P1> receive1(callable: KCallable<*>, context: AsyncCallContext, p1: P1, defaultBody: () -> Unit) {
        val key = SignalKey(callable, context)
        this.receive(callable, context) {
            val wrFunc = this.removeWhenReceived(key) // TODO: what if we process the receive before the whenRecieved...can this happen? and handle timeout!
            if (null != wrFunc) {
                log.trace { "using when received: ${key.signal.name}" }
                (wrFunc as (P1) -> Unit).invoke(p1)
            } else {
                defaultBody.invoke()
            }
        }
    }

    private fun <P1, P2> receive2(callable: KCallable<*>, context: AsyncCallContext, p1: P1, p2: P2, defaultBody: () -> Unit) {
        val key = SignalKey(callable, context)
        this.receive(callable, context) {
            val wrFunc = this.removeWhenReceived(key) // TODO: what if we process the receive before the whenRecieved...can this happen? and handle timeout!
            if (null != wrFunc) {
                log.trace { "using when received: ${key.signal.name}" }
                (wrFunc as (P1, P2) -> Unit).invoke(p1, p2)
            } else {
                defaultBody.invoke()
            }
        }
    }

    private fun <P1, P2, P3> receive3(callable: KCallable<*>, context: AsyncCallContext, p1: P1, p2: P2, p3: P3, defaultBody: () -> Unit) {
        val key = SignalKey(callable, context)
        this.receive(callable, context) {
            val wrFunc = this.removeWhenReceived(key) // TODO: what if we process the receive before the whenRecieved...can this happen? and handle timeout!
            if (null != wrFunc) {
                (wrFunc as (P1, P2, P3) -> Unit).invoke(p1, p2, p3)
            } else {
                defaultBody.invoke()
            }
        }
    }

    override fun receive(callable: KCallable<*>, context: AsyncCallContext, action: suspend () -> Unit) {
        log.trace { "received: ${callable.name}" }
        inbox.offer(Signal(callable, action))
    }

    override suspend fun send(toSend: suspend () -> Unit): AsyncCall {
        return AsyncDefault(this, asyncCallContext(), toSend)
    }

    fun putWhenReceived(key: SignalKey, func: Any) {
        log.trace { "when received: ${key.signal.name}" }
        var set = this.whenReceived[key]
        if (null == set) {
            set = mutableSetOf<Any>()
            this.whenReceived[key] = set
        }
        set.add(func);
    }

    private fun removeWhenReceived(key: SignalKey): Any? {
        val set = this.whenReceived[key]
        if (null != set && set.isNotEmpty()) {
            val func = set.first()
            set.remove(func)
            return func
        }
        return null
    }

    override suspend fun start() {
        log.trace { "start" }
        log.trace { "initialise" }
        this@AFActorDefault.initialise()

        val activeParts = super.framework.partsOf(self).filterIsInstance<Active>()
        activeParts.forEach {
            it.af.start()
        }

        val asyncCallContext = AsyncCallContextDefault(self.af.identity)
        job = GlobalScope.launch(asyncCallContext) {

            log.trace { "preExecute" }
            this@AFActorDefault.preExecute()

            inbox.consumeEach { signal ->
                log.trace { "invoking: ${signal.signature.name}" }
                signal.invoke(self)
            }
            inbox.close()

        }

        activeParts.forEach {
            it.af.join()
        }

    }

    override suspend fun join() {
        log.trace { "join begin" }
        val activeParts = super.framework.partsOf(self).filterIsInstance<Active>()
        activeParts.forEach {
            it.af.join()
        }
        job.join()
        log.trace { "join end" }
    }

    override suspend fun shutdown() {
        log.trace { "shutdown begin" }
        val activeParts = super.framework.partsOf(self).filterIsInstance<Active>()
        activeParts.forEach {
            it.af.shutdown()
            //it.af.join()
        }
        finalise()
        this.inbox.close()
        job.cancel("af.shutdown() called")
        log.trace { "shutdown end" }
    }

    override suspend fun terminate() {
        coroutineContext
        log.trace { "terminate" }
        val activeParts = super.framework.partsOf(self).filterIsInstance<Active>()
        activeParts.forEach {
            it.af.terminate()
        }
        this.inbox.cancel()
        job.cancelAndJoin()
    }

    override fun <T : Any> receiver(forInterface: KClass<*>): T {
        //TODO: cache receivers
        return super.framework.receiver(forInterface) { proxy, callable, args ->
            //TODO: ....maybe it does not matter if self implements the interface..so long as it has an 'andWhen' called?
            val lastArg = args.lastOrNull()
            if (lastArg is Continuation<*>) {
                val asyncCallContext = lastArg.context[AsyncCallContextDefault] ?: TODO()
                when {
                    forInterface.isInstance(self) -> when (args.size) {
                        //TODO: what asyncContext should we use here?
                        0 -> receive0(callable, asyncCallContext) {
                            self.reflect().call(callable.name, *args)
                        }
                        1 -> receive1(callable, asyncCallContext, args[0]) {
                            self.reflect().call(callable.name, *args)
                        }
                        2 -> receive2(callable, asyncCallContext, args[0], args[1]) {
                            self.reflect().call(callable.name, *args)
                        }
                        else -> TODO()
                    }
                    else -> throw ActorException("${self.af.identity}:${self::class.simpleName!!} does not implement ${forInterface.simpleName!!}")
                }
            } else {
                val asyncCallContext = AsyncCallContextDefault("new") //TODO: better id!
                when {
                    forInterface.isInstance(self) -> when (args.size) {
                        //TODO: what asyncContext should we use here?
                        0 -> receive0(callable, asyncCallContext) {
                            self.reflect().call(callable.name, *args)
                        }
                        1 -> receive1(callable, asyncCallContext, args[0]) {
                            self.reflect().call(callable.name, *args)
                        }
                        2 -> receive2(callable, asyncCallContext, args[0], args[1]) {
                            self.reflect().call(callable.name, *args)
                        }
                        else -> TODO()
                    }
                    else -> throw ActorException("${self.af.identity}:${self::class.simpleName!!} does not implement ${forInterface.simpleName!!}")
                }
            }
        }
    }


}

suspend fun asyncCallContext(): AsyncCallContext = coroutineContext[AsyncCallContextDefault]!!

data class AsyncCallContextDefault(
        val contextId: String
) : AsyncCallContext {
    companion object Key : CoroutineContext.Key<AsyncCallContext> {
        init {
            CoroutineExceptionHandler
        }
    }

    override val key = Key

    val signalTrace = mutableListOf<KCallable<*>>()
}

class AsyncDefault(
        val actor: AFActorDefault,
        val context: AsyncCallContext,
        val toSend: suspend () -> Unit
) : AsyncCall {

    var completed: Boolean = false

    override suspend fun go() {
        this.toSend.invoke()
    }

    @ExperimentalTime
    override fun waitUntilFinished(timeout: Duration): AsyncCall.WaitResult {
        TODO()
        /*
        try {
            val msecs = timeout.inMilliseconds
            val startTime = msecs <= 0 ? 0 : MonoClock.
            val waitTime = msecs;
            if (this.completed) {
                return AsyncCall.WaitResult.complete;
            } else if (waitTime <= 0) {
                return AsyncCall.WaitResult.timeout;
            } else {
                for (;;) {
                    synchronized(this) {
                        this.wait(waitTime);
                    }

                    if (this.completed) {
                        return AsyncCall.WaitResult.complete;
                    } else {
                        waitTime = msecs - (System.currentTimeMillis() - startTime);
                        if (waitTime <= 0) {
                            return AsyncCall.WaitResult.timeout;
                        }
                    }
                }
            }
        } catch (e: InterruptedException) {
            return AsyncCall.WaitResult.interrupted;
        }
         */
    }

    @ExperimentalTime
    override fun andWhen(signalSignature: KFunction<*>, timeout: Duration, body: suspend () -> Unit): AsyncCall {
        this.actor.putWhenReceived(SignalKey(signalSignature, this.context), body)
        return this
    }

    @ExperimentalTime
    override fun <P1> andWhen(signalSignature: KFunction<*>, timeout: Duration, body: suspend (p1: P1) -> Unit): AsyncCall {
        this.actor.putWhenReceived(SignalKey(signalSignature, this.context), body)
        return this
    }

    @ExperimentalTime
    override fun <P1, P2> andWhen(signalSignature: KFunction<*>, timeout: Duration, body: suspend (p1: P1, p2: P2) -> Unit): AsyncCall {
        this.actor.putWhenReceived(SignalKey(signalSignature, this.context), body)
        return this
    }

    @ExperimentalTime
    override fun <P1, P2, P3> andWhen(signalSignature: KFunction<*>, timeout: Duration, body: suspend (p1: P1, p2: P2, p3: P3) -> Unit): AsyncCall {
        this.actor.putWhenReceived(SignalKey(signalSignature, this.context), body)
        return this
    }

    @ExperimentalTime
    override fun <P1, P2, P3, P4> andWhen(signalSignature: KFunction<*>, timeout: Duration, body: suspend (p1: P1, p2: P2, p3: P3, p4: P4) -> Unit): AsyncCall {
        this.actor.putWhenReceived(SignalKey(signalSignature, this.context), body)
        return this
    }

    @ExperimentalTime
    override fun <P1, P2, P3, P4, P5> andWhen(signalSignature: KFunction<*>, timeout: Duration, body: suspend (p1: P1, p2: P2, p3: P3, p4: P4, p5: P5) -> Unit): AsyncCall {
        this.actor.putWhenReceived(SignalKey(signalSignature, this.context), body)
        return this
    }

}