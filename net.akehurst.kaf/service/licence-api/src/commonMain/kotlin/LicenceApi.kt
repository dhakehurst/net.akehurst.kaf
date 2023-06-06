/**
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.kaf.service.licence.api

import net.akehurst.kaf.common.api.AFHolder
import net.akehurst.kaf.service.api.Service
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

interface LicenceDetails {
    val featureIdentity: String
    val granted:Boolean
}

interface LicenceService : Service {
    fun get(featureIdentity: String): LicenceDetails
}

fun licenceRequest(featureIdentity:String): LicenceRequest {
    return LicenceRequest(featureIdentity)
}

class LicenceRequest(
    val featureIdentity: String
) : ReadOnlyProperty<AFHolder, LicenceDetails> {

    private lateinit var licenceService: LicenceService

    override fun getValue(thisRef: AFHolder, property: KProperty<*>): LicenceDetails {
        return this.licenceService.get(featureIdentity)
    }
}