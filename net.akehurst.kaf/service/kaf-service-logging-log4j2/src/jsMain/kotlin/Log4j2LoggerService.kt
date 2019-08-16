package net.akehurst.kaf.service.logging.log4j2

import net.akehurst.kaf.service.logging.api.Logger
import net.akehurst.kaf.service.logging.api.LoggingService

class LoggingServiceLog4j2(
) : LoggingService {

    override fun create(identity: String): Logger {
        TODO("This LoggerService is not supported in a Macos64 Native platform")
    }

}