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

package net.akehurst.kaf.service.configuration.map

import com.soywiz.korio.async.runBlockingNoSuspensions
import com.soywiz.korio.file.std.UrlVfs
import com.soywiz.korio.file.std.applicationVfs
import com.soywiz.korio.file.std.rootLocalVfs
import com.soywiz.korio.file.std.uniVfs
import net.akehurst.kaf.service.configuration.api.ConfigurationService

class ConfigurationFile(
    val filePath: String
) : ConfigurationService {

    private val fs = applicationVfs

    private val values : Map<String, Any> by lazy {
        runBlockingNoSuspensions {
            val lines = fs.get(filePath).readLines()

        }

        //TODO:
        mutableMapOf<String, Any>()
    }

    override fun <T> get(path: String, default:()->T): T {
        return this.values[path] as T ?: default()
    }

}