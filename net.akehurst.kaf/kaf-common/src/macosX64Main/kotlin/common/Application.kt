package net.akehurst.kaf.common

import net.akehurst.kaf.api.Application
import net.akehurst.kaf.api.Service

actual inline fun afApplication(self: Application, id: String, init: AFApplication.Builder.() -> Unit): AFApplication {
    val builder = AFApplication.Builder(self, id)
    builder.init()
    return builder.build()
}



actual class AFApplication(

) {
    actual class Builder(
            val self: Application,
            val id: String
    ) {
        actual var initialise: () -> Unit
            get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
            set(value) {}
        actual var execute: () -> Unit
            get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
            set(value) {}
        actual var terminate: () -> Unit
            get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
            set(value) {}
        actual val services: MutableMap<String, Service>
            get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.


        actual fun build() : AFApplication {
            return AFApplication()
        }
    }

}