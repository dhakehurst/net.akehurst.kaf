package net.akehurst.kaf.simple.hellouser.engineering.greeter2gui

import com.soywiz.korge.component.docking.jellyButton
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.ui.Button
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.solidRect
import com.soywiz.korim.color.Colors

class LoginScene : Scene() {
    override suspend fun Container.sceneInit(): Unit {
        solidRect(100, 100, Colors.RED)
    }
}

class MessageScene : Scene() {
    override suspend fun Container.sceneInit(): Unit {
        solidRect(100, 100, Colors.RED)
    }
}