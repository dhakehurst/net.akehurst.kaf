package net.akehurst.kaf.common

import net.akehurst.kaf.api.Application
import net.akehurst.kaf.api.Service

expect inline fun afApplication(self: Application, id: String, init: AFApplication.Builder.() -> Unit = {}): AFApplication

expect class AFApplication {
    class Builder {
        var initialise: () -> Unit
        var execute: () -> Unit
        var terminate: () -> Unit
        val services: MutableMap<String, Service>
        fun build(): AFApplication
    }
}
