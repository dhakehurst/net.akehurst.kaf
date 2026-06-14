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


import com.github.ajalt.clikt.core.GroupableOption
import com.github.ajalt.clikt.core.ParameterHolder
import com.github.ajalt.clikt.parameters.options.OptionWithValues
import com.github.ajalt.clikt.parameters.options.option
import net.akehurst.kaf.service.commandLineHandler.api.CommandLineHandlerService
import kotlin.reflect.KClass
//import com.github.ajalt.clikt.parsers.Parser

class CommandLineHandlerClikt(
        val commandLineArgs: List<String>
) : CommandLineHandlerService {

    private var parsed = false
    private val registered = mutableMapOf<String, GroupableOption>()
    private val holder = object: ParameterHolder {
        override fun registerOption(option: GroupableOption) {
            registered[option.names.first()] = option
        }

    }

    init {
       // CommandLineParser
    }

    override fun <T> get(path: String, default: () -> T?): T? {
        val option = this.registered[path]
        return when(option) {
            null -> default()
            is OptionWithValues<*,*,*> -> option.value as T
            else -> error("Unsupported")
        }
    }

    override fun <T : Any> registerOption(path: String, type: KClass<T>, default: T?, description: String, hidden: Boolean) {
        val existing = this.registered.contains(path)
        if (existing.not()) {
            holder.option(
                    names = arrayOf("--$path"),
                    help = description,
                    hidden = hidden
            )
        } else {
            // TODO: check everything is the same as what is already registered
        }
    }
}