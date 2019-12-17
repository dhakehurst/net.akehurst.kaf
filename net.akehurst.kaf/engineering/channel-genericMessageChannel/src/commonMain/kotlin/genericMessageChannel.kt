package net.akehurst.kaf.engineering.genericMessageChannel

import net.akehurst.kaf.technology.messageChannel.api.ChannelIdentity
import net.akehurst.kaf.technology.messageChannel.api.MessageChannel
import net.akehurst.kotlinx.reflect.isSuspend
import net.akehurst.kotlinx.reflect.proxyFor
import net.akehurst.kotlinx.reflect.reflect
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

fun <T : Any> genericHandler(
        channel: MessageChannel<T>,
        providedInterfaces: List<KClass<*>>,
        receivers: List<Any>
) {


}


inline fun <reified T : Any, U : Any> interface2MessageChannel(noinline channel: () -> MessageChannel<U>, noinline serialise: (args: List<Any>) -> String): T {
    return interface2MessageChannel(T::class, channel, serialise)
}

fun <T : Any, U : Any> interface2MessageChannel(klass: KClass<T>, channel: () -> MessageChannel<U>, serialise: (args: List<Any>) -> String): T {
    return proxyFor(klass) { handler, proxy, callable, methodName, args ->
        when {
            Any::equals == callable -> handler.reflect().call(methodName, *args)
            Any::hashCode == callable -> handler.reflect().call(methodName, *args)
            Any::toString == callable -> "proxy for ${klass}"
            else -> {
                val channelId = ChannelIdentity(callable.name)
                val endPointId = args[0] as U
                val argsL = if (callable is KFunction<*> && callable.isSuspend()) {
                    args.toList().drop(1).dropLast(1)
                } else {
                    args.toList().drop(1)
                }
                val jsonString = serialise(argsL)
                channel().send(endPointId, channelId, jsonString)
            }
        }
    }
}


inline suspend fun <reified T : Any, U : Any> messageChannel2Interface(receiver: T, channel: MessageChannel<U>, noinline deserialise: (String) -> List<Any>) {
    messageChannel2Interface(T::class, receiver, channel, deserialise)
}

suspend fun <T : Any, U : Any> messageChannel2Interface(klass: KClass<T>, receiver: T, channel: MessageChannel<U>, deserialise: (String) -> List<Any>) {
    klass.reflect().allMemberFunctions.forEach { mf ->
        //TODO: is test by name ok ? probably not
        if (Any::class.reflect().allMemberFunctions.any { it.name == mf.name }) {
            //do nothing
        } else {
            val channelId = ChannelIdentity(mf.name)
            channel.receive(channelId) { endPointId, message ->
                val args = deserialise(message) as List<*>
                val callArgs = listOf(endPointId) + args
                if (mf.isSuspend()) {
                    receiver.reflect().callSuspend(channelId.value, *callArgs.toTypedArray())
                } else {
                    receiver.reflect().call(channelId.value, *callArgs.toTypedArray())
                }
            }
        }
    }
}