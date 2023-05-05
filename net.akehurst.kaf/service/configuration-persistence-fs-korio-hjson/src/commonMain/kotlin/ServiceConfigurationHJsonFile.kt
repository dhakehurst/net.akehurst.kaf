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

package net.akehurst.kaf.service.configuration.hjson

import net.akehurst.hjson.HJson
import net.akehurst.hjson.HJsonDocument
import net.akehurst.hjson.HJsonObject
import net.akehurst.kaf.common.api.AFOwner
import net.akehurst.kaf.common.api.Owner
import net.akehurst.kaf.common.api.Passive
import net.akehurst.kaf.common.realisation.afPassive
import net.akehurst.kaf.service.configuration.api.ConfigurationService
import net.akehurst.kaf.technology.persistence.fs.api.PersistenceFilesystem
import net.akehurst.kaf.technology.persistence.fs.korio.PersistenceFSKorioUniFS
import net.akehurst.kotlin.kserialisation.hjson.KSerialiserHJson


class ServiceConfigurationHJsonFile(
        val filePath: String,
        /**
         * if true then load the hjson doc fresh each time a value is accessed.
         * This enables conf to be changed whilst application is running, though there
         * is of course a performance hit. Default is false
         */
        val dynamic: Boolean = false
) : ConfigurationService, Passive {

    override val af = afPassive()

    var fs: PersistenceFilesystem = PersistenceFSKorioUniFS()

    private val serialiser = KSerialiserHJson()

    val hjson: HJsonDocument by lazy {
        val bytes = fs.read(filePath)
        HJson.process(bytes.decodeToString())
    }

    init {
        this.serialiser.registerKotlinStdPrimitives()
    }

    override fun <T : Any> get(path: String, default: () -> T): T {
        val pathList = path.split('.')
        val parentPath = pathList.dropLast(1)
        val propInParent = pathList.last()
        val hjObj = if (dynamic) {
            val bytes = fs.read(filePath)
            val str = bytes.decodeToString()
            val hj = HJson.process(str)
            val parentObj = (hj.index[parentPath] as HJsonObject?)
            parentObj?.property?.get(propInParent)
        } else {
            val parentObj = (this.hjson.index[parentPath] as HJsonObject?)
            parentObj?.property?.get(propInParent)
        }
        return if (null == hjObj) {
            default.invoke()
        } else {
            val str = hjObj.toHJsonString("  ", "  ")
            val value: T = serialiser.toData(str)
            value
        }
    }

}