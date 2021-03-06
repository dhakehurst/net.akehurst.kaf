/**
 * Copyright (C) 2019 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.akehurst.kaf.service.logging.console

import net.akehurst.kaf.service.logging.api.*

class LoggingServiceConsole(
        initLevel: LogLevel = LogLevel.INFO
) : LoggingService {

    var outputLevel = initLevel

    override fun create(identity: String): Logger {
        return LoggerConsole(this, identity)
    }

}

internal class LoggerConsole(
        val service: LoggingServiceConsole,
        val identity: String
) : Logger {

    override fun log(level: LogLevel, throwable: Throwable?, message: () -> String?) {
        if (level.value >= this.service.outputLevel.value) {
            val out = "[$level]: ($identity) ${message()}"
            println(out)
            if (null!=throwable) {
                println(throwable)
            }
        }
    }

}