package net.akehurst.kaf.common.api

import kotlin.reflect.KClass

class ApplicationInstantiationException(message: String) : RuntimeException(message)
class ActiveException(message: String) : RuntimeException(message)
class ActorException(message: String) : RuntimeException(message)

class ServiceNotFoundException(serviceClass: KClass<*>) : RuntimeException("Service with identity ${serviceClass.simpleName} is not found")


