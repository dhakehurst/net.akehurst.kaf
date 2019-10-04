package net.akehurst.kaf.common

import net.akehurst.kaf.api.Identifiable

expect class ApplicationFrameworkService {
    fun doInjections(commandLineArgs: List<String>, root: Identifiable)
}

