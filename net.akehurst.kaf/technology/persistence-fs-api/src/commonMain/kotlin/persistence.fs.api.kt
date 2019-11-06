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

package net.akehurst.kaf.technology.persistence.fs.api


open class PersistenceFilesystemException : RuntimeException {
    constructor(message: String) : super(message)
}

class File(
        val uri: String,
        var content: Any? = null
) {
    override fun hashCode(): Int = uri.hashCode()
    override fun equals(other: Any?): Boolean {
        return when {
            other is File -> this.uri == other.uri
            else -> false
        }
    }

    override fun toString(): String = "File{uri=$uri}"
}

interface PersistenceFilesystem {

    fun write(uri: String, bytes:ByteArray)

    fun read(uri: String): ByteArray
    fun read(uri: String, offset: Long, size: Int): Sequence<ByteArray>

}
