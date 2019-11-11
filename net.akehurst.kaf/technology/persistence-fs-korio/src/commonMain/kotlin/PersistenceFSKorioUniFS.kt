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

package net.akehurst.kaf.technology.persistence.fs.korio

import com.soywiz.korio.file.std.uniVfs
import net.akehurst.kaf.common.api.Component
import net.akehurst.kaf.common.api.Owner
import net.akehurst.kaf.common.api.Port
import net.akehurst.kaf.common.realisation.afComponent
import net.akehurst.kaf.common.realisation.runBlocking
import net.akehurst.kaf.technology.persistence.fs.api.PersistenceFilesystem
import net.akehurst.kotlin.komposite.common.DatatypeRegistry

class PersistenceFSKorioUniFS(
        override val owner: Owner,
        afId: String
) : Component, PersistenceFilesystem {

    lateinit var port_fs: Port

    // --- KAF ---
    override val af = afComponent(this, afId) {
        port_fs = port("fs") {
            provides(PersistenceFilesystem::class)
        }
        initialise = {
            port_fs.connectInternal(self)
        }
    }

    // --- PersistentStore ---

    override fun write(uri: String, bytes: ByteArray) {
        runBlocking {
            uri.uniVfs.writeBytes(bytes)
        }
    }

    override fun read(uri: String): ByteArray {
        return runBlocking {
            uri.uniVfs.readBytes()
        }
    }

    override fun read(uri: String, offset: Long, size: Int): Sequence<ByteArray> {
        return sequence {
            val chunk = runBlocking {
                uri.uniVfs.readChunk(offset, size)
            }
            yield(chunk)
        }
    }


}