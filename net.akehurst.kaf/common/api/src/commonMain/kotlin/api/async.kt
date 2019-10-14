package net.akehurst.kaf.common.api

import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KFunction
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime


interface AsyncCallContext : CoroutineContext.Element {
}

interface AsyncCall {
    enum class WaitResult {
        complete, timeout, interrupted
    }

    /**
     * do the call now
     */
    suspend fun go()

    /**
     *
     * @param timeout
     * @param unit
     * @return result of waiting
     */
    @ExperimentalTime
    fun waitUntilFinished(timeout: Duration): WaitResult

    @ExperimentalTime
    fun andWhen(signalSignature: KFunction<*>, timeout: Duration, body: suspend () -> Unit): AsyncCall

    @ExperimentalTime
    fun <P1> andWhen(signalSignature: KFunction<*>, timeout: Duration, body: suspend (p1: P1) -> Unit): AsyncCall

    @ExperimentalTime
    fun <P1, P2> andWhen(signalSignature: KFunction<*>, timeout: Duration, body: suspend (p1: P1, p2: P2) -> Unit): AsyncCall

    @ExperimentalTime
    fun <P1, P2, P3> andWhen(signalSignature: KFunction<*>, timeout: Duration, body: suspend (p1: P1, p2: P2, p3: P3) -> Unit): AsyncCall

    @ExperimentalTime
    fun <P1, P2, P3, P4> andWhen(signalSignature: KFunction<*>, timeout: Duration, body: suspend (p1: P1, p2: P2, p3: P3, p4: P4) -> Unit): AsyncCall

    @ExperimentalTime
    fun <P1, P2, P3, P4, P5> andWhen(signalSignature: KFunction<*>, timeout: Duration, body: suspend (p1: P1, p2: P2, p3: P3, p4: P4, p5: P5) -> Unit): AsyncCall
}