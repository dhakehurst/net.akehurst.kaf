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

package cl.simple

import net.akehurst.kaf.simple.hellouser.technology.cl.api.Console
import net.akehurst.kaf.simple.hellouser.technology.cl.simple.ConsoleSimple
import kotlin.test.Test
import kotlin.test.assertEquals

class test_ConsoleSimple {

    @Test
    fun stdout_write() {

        val sut:Console = ConsoleSimple("sut")

        sut.stdout.write("Test")


    }


}