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

package net.akehurst.hjson

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith


class test_HJsonToString {

    @Test
    fun boolean_false() {

        val json = hjson("json") {
            primitive(false)
        }

        val actual = json.toHJsonString()

        val expected = "false"

        assertEquals(expected, actual)

    }

    @Test
    fun boolean_true() {

        val json = hjson("json") {
            primitive(true)
        }

        val actual = json.toHJsonString()

        val expected = "true"

        assertEquals(expected, actual)

    }

    @Test
    fun number_1() {

        val json = hjson("json") {
            primitive(1)
        }

        val actual = json.toHJsonString()

        val expected = "1"

        assertEquals(expected, actual)

    }

    @Test
    fun string_empty() {

        val json = hjson("json") {
            primitive("")
        }

        val actual = json.toHJsonString()

        val expected = """"""""

        assertEquals(expected, actual)

    }

    @Test
    fun string_multiline() {

        val json = hjson("json") {
            primitive("""
                hello
                world
            """.trimIndent()
            )
        }

        val actual = json.toHJsonString()

        val expected = """
            '''hello
            world'''
        """.trimIndent()

        assertEquals(expected, actual)

    }

    @Test
    fun string() {

        val json = hjson("json") {
            primitive("hello world")
        }

        val actual = json.toHJsonString()

        val expected = "hello world"

        assertEquals(expected, actual)

    }

    @Test
    fun array_empty() {

        val json = hjson("json") {
            arrayJson {}
        }

        val actual = json.toHJsonString()

        val expected = "[]"

        assertEquals(expected, actual)

    }

    @Test
    fun array_1() {

        val json = hjson("json") {
            arrayJson {
                primitive(1)
            }
        }

        val actual = json.toHJsonString()

        val expected = """
            [ 1 ]
        """.trimIndent()

        assertEquals(expected, actual)

    }

    @Test
    fun array_many() {

        val json = hjson("json") {
            arrayJson {
                primitive(1)
                primitive(true)
                primitive("hello")
                objectJson { }
            }
        }

        val actual = json.toHJsonString()

        val expected = """
            [
              1
              true
              hello
              {}
            ]
        """.trimIndent()

        assertEquals(expected, actual)

    }

    @Test
    fun object_empty() {

        val json = hjson("json") {
            objectJson { }
        }

        val actual = json.toHJsonString()

        val expected = "{}"

        assertEquals(expected, actual)

    }

    @Test
    fun object_() {

        val json = hjson("json") {
            objectJson {
                property("bProp", true)
                property("nProp", 1)
                property("sProp", "hello")
                property("aProp") {
                    arrayJson {
                        primitive(1)
                        primitive(true)
                        primitive("hello")
                        objectJson { }
                    }

                }
                property("oProp") {
                    objectJson {
                        property("bProp", false)
                        property("nProp", 3.14)
                    }
                }
            }
        }

        val actual =json.toHJsonString()

        val expected = """
        {
          bProp : true
          nProp : 1
          sProp : hello
          aProp : [
            1
            true
            hello
            {}
          ]
          oProp : {
            bProp : false
            nProp : 3.14
          }
        }
        """.trimIndent()

        assertEquals(expected, actual)

    }

}