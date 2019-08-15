package net.akehurst.kaf.service.commandLineHandler.clikt

import com.github.ajalt.clikt.completion.CompletionCandidates
import com.github.ajalt.clikt.core.NoRunCliktCommand
import com.github.ajalt.clikt.core.ParameterHolder
import com.github.ajalt.clikt.core.findObject
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parsers.OptionWithValuesParser
import net.akehurst.kaf.service.commandLineHandler.api.CommandLineHandler
import net.akehurst.kaf.service.commandLineHandler.api.commandLineValue
import kotlin.reflect.KClass

class CommandLineHandlerClikt(
        val commandLineArgs: List<String>,
        clikt: NoRunCliktCommand = NoRunCliktCommand()
) : CommandLineHandler {

    private var _clikt: NoRunCliktCommand = clikt
    private var parsed = false

    val clikt: NoRunCliktCommand
        get() {
            if (!parsed) {
                this._clikt.main(this.commandLineArgs)
            }
            return this._clikt
        }

    override fun <T> get(path: String, default: () -> T?): T? {
        val option = this.clikt.findOption("--$path")
        val value = if (null == option) null else (option as OptionWithValues<T, Any, Any>).value
        return value ?: default()
    }

    override fun <T : Any> registerOption(path: String, type: KClass<T>, default: T?, description: String, hidden: Boolean) {
        val existing = this._clikt.findOption(path)
        if (null==existing) {
            this._clikt.registerOption(
                    OptionWithValues<T?, Any, Any>(
                            names = setOf("--$path"),
                            metavarWithDefault = ValueWithDefault("the greeting", default.toString()),
                            nvalues = 1,
                            help = description,
                            hidden = hidden,
                            helpTags = emptyMap(),
                            envvar = null,
                            envvarSplit = ValueWithDefault(null, Regex("\\s+")),
                            valueSplit = null,
                            parser = OptionWithValuesParser,
                            completionCandidates = CompletionCandidates.None,
                            transformValue = { it },
                            transformEach = { it.single() },
                            transformAll = { it.lastOrNull() as T? },
                            transformValidator = {}
                    )

            )
        } else {
            // TODO: check everything is the same as what is already registered
        }
    }
}