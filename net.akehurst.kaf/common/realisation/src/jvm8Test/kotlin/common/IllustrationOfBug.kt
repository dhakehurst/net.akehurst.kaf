package bug.illustration

import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.kotlinFunction

data class DataClass(val value: String)
inline class InlineClass(val value: String)

class Test {
    fun function1(param: DataClass) {
        println(param)
    }

    fun function2(param: InlineClass) {
        println(param)
    }

    suspend fun sfunction1(param: DataClass) {
        println(param)
    }

    suspend fun sfunction2(param: InlineClass) {
        println(param)
    }
}

fun main() {

    val test = Test()

    // normal function, data class
    val method_1 = Test::function1.javaMethod
    val callable_1 = method_1?.kotlinFunction  // this is ok

    // normal function, inline class
    val method_2 = Test::function2.javaMethod
    val callable_2 = method_2?.kotlinFunction  // this is ok

    // suspend function, data class
    val method_s1 = Test::sfunction1.javaMethod
    val callable_s1 = method_s1?.kotlinFunction  // this is ok

    // suspend function, inline class
    val method_s2 = Test::sfunction2.javaMethod
    val callable_s2 = method_s2?.kotlinFunction  // this fails


}