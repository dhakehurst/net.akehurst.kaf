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

package net.akehurst.kaf.sample.hellouser.application.desktop.cl.common


import net.akehurst.kaf.common.*
import net.akehurst.kaf.service.logging.console.*

import net.akehurst.kaf.sample.hellouser.computational.greeter.simple.GreeterSimple
import net.akehurst.kaf.service.logging.api.LogLevel
import net.akehurst.kaf.simple.hellouser.engineering.greeter2cl.Greeter2Cl
import net.akehurst.kaf.simple.hellouser.technology.cl.simple.ConsoleSimple

class DesktopApplication :Application {

    // --- services ---

    //--- computational ---
    @Composite
    val greeter = GreeterSimple("greeter")

    //--- engineering ---
    @Composite
    val user2cl = Greeter2Cl("user2cl")

    //--- technology ---
    @Composite
    val console = ConsoleSimple("console")

    override  val af = afApplication(this, "desktop") {
        services["logger"] = LoggerConsole(LogLevel.ALL)

        initialise = {
            greeter.out = user2cl
            user2cl.console = console
            user2cl.greeterRequest = greeter
        }
        execute = {
            greeter.start()
        }
    }



}