package net.akehurst.kaf.common

import net.akehurst.kaf.api.AFIdentifiable
import net.akehurst.kaf.service.api.serviceReference
import net.akehurst.kaf.service.logging.api.Logger

class ApplicationInstantiationException(message:String) : Exception(message)
class ServiceNotFoundException(sid:String) : Exception("Service with identity $sid is not found")


open class AFBase(
        override  val identity: String
)  : AFIdentifiable {

    override val logger:Logger by serviceReference<Logger>("logger")

    override fun toString(): String {
        return "AF{$identity}"
    }
}

