package net.akehurst.kaf.service.configuration.map

import net.akehurst.kaf.service.configuration.api.Configuration

class ConfigurationMap(
        val values:MutableMap<String,Any>
) : Configuration {
    override fun <T> get(path: String, default:()->T): T {
        return this.values[path] as T ?: default()
    }
}