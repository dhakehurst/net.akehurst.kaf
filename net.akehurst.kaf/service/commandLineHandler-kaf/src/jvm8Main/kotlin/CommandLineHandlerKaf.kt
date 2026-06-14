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

package net.akehurst.kaf.service.commandLineHandler.clikt


import net.akehurst.kaf.service.commandLineHandler.api.CommandLineHandlerService
import kotlin.reflect.KClass

data class Option(
        val path: String
) {
    var value: Any? = null
}

class CommandLineHandlerClikt(
        val commandLineArgs: List<String>
) : CommandLineHandlerService {

    private var parsed = false
    private val registered = mutableMapOf<String, Option>()

    override fun <T> get(path: String, default: () -> T?): T? {
        val option = this.registered[path]
        val value = if (null == option) null else option.value
        return value as T ?: default()
    }

    override fun <T : Any> registerOption(path: String, type: KClass<T>, default: T?, description: String, hidden: Boolean) {
        val existing = this.registered.contains(path)
        if (existing.not()) {
            /*
            val option = OptionWithValues<T?, Any, Any>(
                    names = setOf("--$path"),
                    metavarDefault = "the greeting",
                    nvalues = 1,
                    help = description,
                    hidden = hidden,
                    // helpTags = emptyMap(),
                    envvar = null,
                    envvarSplit = Regex("\\s+"),
                    // valueSplit = null,
                    parser = OptionWithValuesParser,
                    //completionCandidates = CompletionCandidates.None,
                    transformValue = { it },
                    transformEach = { it.single() },
                    transformAll = { it.lastOrNull() as T? }
                    // transformValidator = {}
            )
             */
            val option = Option(path)
            this.registered[path] = option
        } else {
            // TODO: check everything is the same as what is already registered
        }
    }
}