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