package net.akehurst.kaf.common

import net.akehurst.kaf.api.AFActive
import net.akehurst.kaf.api.AFIdentifiable
import net.akehurst.kaf.api.Active
import net.akehurst.kaf.api.Identifiable
import net.akehurst.kaf.service.api.serviceReference
import net.akehurst.kaf.service.logging.api.logger

class ApplicationInstantiationException(message: String) : Exception(message)
class ServiceNotFoundException(sid: String) : Exception("Service with identity $sid is not found")

inline fun afIdentifiable(self: Identifiable, id: String, init: AFIdentifiableDefault.Builder.() -> Unit = {}): AFIdentifiable {
    val builder = AFIdentifiableDefault.Builder(self, id)
    builder.init()
    return builder.build()
}

open class AFIdentifiableDefault(
        override val identity: String
) : AFIdentifiable {

    class Builder(val self: Identifiable, val id: String) {
        fun build(): AFIdentifiable {
            return AFIdentifiableDefault(id)
        }
    }

    val framework by serviceReference<ApplicationFrameworkService>("framework")
    override val log by logger("logging")

    override fun doInjections(root:Identifiable) {
        framework.doInjections(emptyList<String>(),root)
    }

    override fun hashCode(): Int {
        return this.identity.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return when(other) {
            is AFIdentifiable -> this.identity==other.identity
            else -> false
        }
    }
    override fun toString(): String {
        return "AF{$identity}"
    }
}

