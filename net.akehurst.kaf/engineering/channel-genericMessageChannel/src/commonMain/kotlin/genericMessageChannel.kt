package net.akehurst.kaf.engineering.genericMessageChannel

import net.akehurst.kaf.technology.messageChannel.api.ChannelIdentity
import net.akehurst.kaf.technology.messageChannel.api.MessageChannel
import net.akehurst.kotlinx.reflect.isSuspend
import net.akehurst.kotlinx.reflect.proxyFor
import net.akehurst.kotlinx.reflect.reflect
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.reflect.KFunction

inline  fun <reified T : Any, U : Any> interface2MessageChannel(crossinline channel:()-> MessageChannel<U>, crossinline serialise: (args: List<Any>) -> String): T {
    return proxyFor(T::class) { handler, proxy, callable, methodName, args ->
        when {
            Any::equals == callable -> handler.reflect().call(methodName, *args)
            Any::hashCode == callable -> handler.reflect().call(methodName, *args)
            Any::toString == callable -> "proxy for ${T::class}"
            else -> {
                val channelId = ChannelIdentity(callable.name)
                val endPointId = args[0] as U
                val argsL =  if (callable is KFunction<*> && callable.isSuspend()) {
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


inline suspend fun <reified T : Any, U : Any> messageChannel2Interface(receiver:T, channel: MessageChannel<U>, crossinline deserialise: (String) -> List<Any>) {
    T::class.reflect().allMemberFunctions.forEach {
        //TODO: eliminate Any::.... functions
        val channelId = ChannelIdentity(it.name)
        channel.receive(channelId) { endPointId, message ->
            val args = deserialise(message) as List<*>
            if (it.isSuspend()) {
                suspendCoroutineUninterceptedOrReturn { cont ->
                    val callArgs = listOf(endPointId) + args + cont
                    receiver.reflect().call(channelId.value, *callArgs.toTypedArray())
                }
            } else {
                val callArgs = listOf(endPointId) + args
                receiver.reflect().call(channelId.value, *callArgs.toTypedArray())
            }
        }
    }
}