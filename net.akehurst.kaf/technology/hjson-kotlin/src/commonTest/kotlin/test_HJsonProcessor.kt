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


class test_HJsonProcessor {

    @Test
    fun empty() {

        val jsonString = ""

        assertFailsWith<HJsonParserException> {
            val actual = HJson.process(jsonString)
        }

    }

    @Test
    fun boolean_false() {

        val jsonString = "false"

        val actual = HJson.process(jsonString)

        val expected = hjson("json") {
            primitive(false)
        }

        assertEquals(expected, actual)

    }

    @Test
    fun boolean_true() {

        val jsonString = "true"

        val actual = HJson.process(jsonString)

        val expected = hjson("json") {
            primitive(true)
        }

        assertEquals(expected, actual)

    }

    @Test
    fun number_1() {

        val jsonString = "1"

        val actual = HJson.process(jsonString)

        val expected = hjson("json") {
            primitive(1)
        }

        assertEquals(expected, actual)

    }

    @Test
    fun string() {

        val jsonString = "\"hello\""

        val actual = HJson.process(jsonString)

        val expected = hjson("json") {
            primitive("hello")
        }

        assertEquals(expected, actual)

    }

    @Test
    fun multiline_string() {

        val jsonString = """
            '''hello
            world'''
        """.trimIndent()

        val actual = HJson.process(jsonString)

        val expected = hjson("json") {
            primitive("""
                hello
                world
            """.trimIndent())
        }

        assertEquals(expected, actual)

    }

    @Test
    fun emptyArray() {

        val jsonString = "[]"

        val actual = HJson.process(jsonString)

        val expected = hjson("json") {
            arrayJson {}
        }

        assertEquals(expected, actual)

    }

    @Test
    fun array() {

        val jsonString = "[ 1, true, \"hello\", {} ]"

        val actual = HJson.process(jsonString)

        val expected = hjson("json") {
            arrayJson {
                primitive(1)
                primitive(true)
                primitive("hello")
                objectJson { }
            }
        }

        assertEquals(expected, actual)

    }

    @Test
    fun emptyObject() {

        val jsonString = "{}"

        val actual = HJson.process(jsonString);

        val expected = hjson("json") {
            objectJson { }
        }

        assertEquals(expected, actual)

    }

    @Test
    fun emptyObject_with_comments() {

        val jsonString = """
        {
            #hash comment
            # and another
            
            // single line comment
            // and a second one
            
            /* multi line 
               comment also
            */
        }
        """.trimIndent()

        val actual = HJson.process(jsonString)

        val expected = hjson("json") {
            objectJson { }
        }

        assertEquals(expected, actual)

    }

    @Test
    fun object_json() {

        val jsonString = """
            {
                "bProp" : true,
                "nProp" : 1,
                "sProp" : "hello",
                "aProp" : [ 1, true, "hello", {} ],
                "oProp" : {
                    "bProp": false,
                    "nProp" : 3.14
                }
            }
        """.trimIndent()

        val actual = HJson.process(jsonString);

        val expected = hjson("json") {
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

        assertEquals(expected, actual)

    }
    @Test
    fun object_hjson() {

        val jsonString = """
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

        val actual = HJson.process(jsonString);

        val expected = hjson("json") {
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

        assertEquals(expected, actual)

    }
}