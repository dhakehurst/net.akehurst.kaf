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

package net.akehurst.kaf.simple.hellouser.technology.cl.simple

import net.akehurst.kaf.simple.hellouser.technology.cl.api.Console
import net.akehurst.kaf.simple.hellouser.technology.cl.api.Environment
import net.akehurst.kaf.simple.hellouser.technology.cl.api.OutputStream

class ConsoleSimple(afIdentity:String) : Console {

    override val stdout = object : OutputStream {
        override fun write(content: String) {
            print(content)
        }
    }

    override val environment = object :Environment {
        override val variable:Map<String,String> get() {
            return mapOf("USER" to "dha")
        }
    }
}