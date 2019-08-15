package net.akehurst.kaf.common

import net.akehurst.kaf.api.AFIdentifiable
import net.akehurst.kaf.service.logging.api.logger

class ApplicationInstantiationException(message: String) : Exception(message)
class ServiceNotFoundException(sid: String) : Exception("Service with identity $sid is not found")


open class AFBase(
        override val identity: String
) : AFIdentifiable {

    override val log by logger("logging")


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

