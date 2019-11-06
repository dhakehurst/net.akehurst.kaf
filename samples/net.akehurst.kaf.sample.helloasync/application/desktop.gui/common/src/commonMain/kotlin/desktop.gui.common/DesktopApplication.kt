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

package net.akehurst.kaf.sample.hellouser.application.desktop.gui.common

import net.akehurst.kaf.sample.hellouser.computational.greeter.simple.GreeterSimple
import net.akehurst.kaf.simple.hellouser.engineering.greeter2gui.Greeter2Gui


class DesktopApplication {

    //--- computational ---
    val greeter = GreeterSimple()

    //--- engineering ---
    val user2gui = Greeter2Gui()

    //--- technology ---


    fun start() {
        greeter.out = user2gui
        user2gui.greeterRequest = greeter

        user2gui.start()
        greeter.start()
    }

}