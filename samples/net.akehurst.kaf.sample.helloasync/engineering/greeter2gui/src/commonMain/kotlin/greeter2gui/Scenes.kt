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

package net.akehurst.kaf.simple.hellouser.engineering.greeter2gui

import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.solidRect
import com.soywiz.korge.view.text
import com.soywiz.korim.color.Colors

class StartScene : Scene() {
    override suspend fun Container.sceneInit(): Unit {
        text("Starting")
        sceneContainer
    }
}

class LoginScene : Scene() {
    override suspend fun Container.sceneInit(): Unit {
        solidRect(100, 100, Colors.RED)
            //TODO: add form showing labels, input for username and password & button
    }
}

class MessageScene : Scene() {

    var text = ""

    override suspend fun Container.sceneInit(): Unit {
        text(text)
    }


}